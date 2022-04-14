package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.exception.GrandTotalTaxNotFoundException;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.OrderRowNotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.CalculationUtil;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;
import static java.math.RoundingMode.HALF_UP;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toUnmodifiableSet;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderRowService {

    public static final String ERROR_MSG_ROW_NOT_FOUND_BY_SKU =
            "Could not find order row with SKU {0} for order number {1} and order group id {2}";
    public static final String ERROR_MSG_GRAND_TOTAL_TAX_NOT_FOUND_BY_TAX_RATE =
            "Could not find order row with SKU {0} and tax rate {1} for order number {2} and order group id {3}";
    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final TimedPollingService timedPollingService;

    @NonNull
    private final OrderUtil orderUtil;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final InvoiceService invoiceService;

    public void cancelOrderProcessIfFullyCancelled(SalesOrder salesOrder) {

        if (isOrderFullyCancelled(salesOrder.getLatestJson())) {
            log.info("Order with order number: {} is fully cancelled, cancelling the order process", salesOrder.getOrderNumber());
            for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
                if (!helper.isShipped(orderRow.getShippingType())) {
                    orderRow.setIsCancelled(true);
                }
            }
            salesOrderService.save(orderUtil.removeCancelledOrderRowsFromLatestJson(salesOrder), Action.ORDER_CANCELLED);
            correlateMessageForOrderCancellation(salesOrder.getOrderNumber());
        }
    }

    @Transactional
    public void handleDropshipmentPurchaseOrderBooked(DropshipmentPurchaseOrderBookedMessage message) {

        String orderNumber = message.getSalesOrderNumber();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        salesOrder.getLatestJson().getOrderHeader().setOrderNumberExternal(message.getExternalOrderNumber());
        salesOrder = salesOrderService.save(salesOrder, DROPSHIPMENT_PURCHASE_ORDER_BOOKED);
        if (!message.getBooked()) {
            for (OrderRows orderRows : ((Order) salesOrder.getOriginalOrder()).getOrderRows()) {
                cancelOrderRow(orderNumber, orderRows.getSku());
            }
            if (timedPollingService.pollWithDefaultTiming(() -> helper.checkIfActiveProcessExists(orderNumber))) {
                cancelOrderProcessIfFullyCancelled(salesOrder);
            }
        }
    }

    public void handleDropshipmentShipmentConfirmed(DropshipmentShipmentConfirmedMessage message) {

        var orderNumber = message.getSalesOrderNumber();
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        var orderRows = salesOrder.getLatestJson().getOrderRows();
        var shippedItems = message.getItems();

        shippedItems.forEach(item ->
                orderRows.stream()
                        .filter(row -> StringUtils.pathEquals(row.getSku(), item.getProductNumber()))
                        .findFirst()
                        .ifPresentOrElse(row -> {
                            addParcelNumber(item, row);
                            addServiceProviderName(item, row);
                        }, () -> {
                            throw new NotFoundException(
                                    format("Could not find order row with SKU {0} for order {1}",
                                            item.getProductNumber(), orderNumber));
                        })
        );

        setDocumentRefNumber(salesOrder);
        var persistedSalesOrder = salesOrderService.save(salesOrder, ORDER_ITEM_SHIPPED);

        var trackingLinks = shippedItems.stream()
                .map(ShipmentItem::getTrackingLink)
                .collect(toUnmodifiableSet());

        snsPublishService.publishSalesOrderShipmentConfirmedEvent(persistedSalesOrder, trackingLinks);
    }

    private void addParcelNumber(ShipmentItem item, OrderRows row) {
        var parcelNumber = item.getParcelNumber();
        Optional.ofNullable(row.getTrackingNumbers())
                .ifPresentOrElse(trackingNumbers -> trackingNumbers.add(parcelNumber),
                        () -> row.setTrackingNumbers(List.of(parcelNumber)));
    }

    private void addServiceProviderName(ShipmentItem item, OrderRows row) {
        row.setShippingProvider(item.getServiceProviderName());
    }

    private void setDocumentRefNumber(SalesOrder salesOrder) {
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceService.createInvoiceNumber());
    }

    @SneakyThrows
    @Transactional
    public void cancelOrderRows(String orderNumber, List<String> skuList) {

        for (String sku : skuList) {
            List<String> originalOrderSkus = getOriginalOrderSkus(orderNumber);
            if (originalOrderSkus.contains(sku)) {
                cancelOrderRow(orderNumber, sku);
            } else {
                log.error("Sku: {} is not in original order with order number: {}", sku, orderNumber);
            }
        }
    }

    public SalesOrderReturn handleSalesOrderReturn(String orderNumber, Collection<CreditNoteLine> creditNoteLines) {
        var salesOrder = salesOrderService.findLastOrderByOrderGroupId(orderNumber);
        var negativedCreditNoteLine = negateCreditNoteLines(creditNoteLines);
        var items = negativedCreditNoteLine.stream()
                .filter(creditNoteLine -> !creditNoteLine.getIsShippingCost())
                .collect(Collectors.toList());
        var returnOrderJson = recalculateOrderByReturns(salesOrder, items);

        returnOrderJson.getOrderHeader().setOrderNumber(salesOrder.getOrderNumber());

        var salesOrderReturn = SalesOrderReturn.builder()
                .orderGroupId(orderNumber)
                .orderNumber(orderNumber)
                .returnOrderJson(returnOrderJson)
                .build();

        salesOrderReturn.setOrderNumber(orderNumber);
        updateShippingCosts(salesOrderReturn, negativedCreditNoteLine);
        salesOrderService.addSalesOrderReturn(salesOrder, salesOrderReturn);

        return salesOrderReturn;
    }

    public Order recalculateOrderByReturns(SalesOrder salesOrder, Collection<CreditNoteLine> items) {

        var returnLatestJson = orderUtil.copyOrderJson(salesOrder.getLatestJson());
        var totals = returnLatestJson.getOrderHeader().getTotals();

        items.forEach(item -> {
            var orderRow = returnLatestJson.getOrderRows().stream()
                    .filter(r -> StringUtils.pathEquals(r.getSku(), item.getItemNumber()))
                    .findFirst()
                    .orElseThrow(() -> new OrderRowNotFoundException(ERROR_MSG_ROW_NOT_FOUND_BY_SKU,
                            item.getItemNumber(), salesOrder.getOrderNumber(), salesOrder.getOrderGroupId()));
            orderUtil.recalculateOrderRow(orderRow, item);

            var sumValues = orderRow.getSumValues();
            var returnOrderRowTaxValue = sumValues.getTotalDiscountedGross().subtract(sumValues.getTotalDiscountedNet());
            totals.getGrandTotalTaxes().stream()
                    .filter(tax -> tax.getRate().compareTo(item.getLineTaxAmount()) == 0)
                    .findFirst()
                    .ifPresentOrElse(tax -> {
                                var taxValue = returnOrderRowTaxValue.compareTo(BigDecimal.ZERO) == 0 ? returnOrderRowTaxValue :
                                        tax.getValue().subtract(returnOrderRowTaxValue);
                                tax.setValue(taxValue);
                            },
                            () -> {
                                throw new GrandTotalTaxNotFoundException(
                                        format(ERROR_MSG_GRAND_TOTAL_TAX_NOT_FOUND_BY_TAX_RATE,
                                                item.getItemNumber(), item.getLineTaxAmount(), salesOrder.getOrderNumber(), salesOrder.getOrderGroupId()));
                            });
        });

        totals.setGoodsTotalGross(BigDecimal.ZERO);
        totals.setGoodsTotalNet(BigDecimal.ZERO);
        totals.setTotalDiscountGross(BigDecimal.ZERO);
        totals.setTotalDiscountNet(BigDecimal.ZERO);
        totals.setGrandTotalGross(BigDecimal.ZERO);
        totals.setGrandTotalNet(BigDecimal.ZERO);
        totals.setPaymentTotal(BigDecimal.ZERO);

        returnLatestJson.getOrderRows().stream()
                .map(OrderRows::getSumValues)
                .forEach(sumValues -> {
                    totals.setGoodsTotalGross(totals.getGoodsTotalGross().add(sumValues.getGoodsValueGross()));
                    totals.setGoodsTotalNet(totals.getGrandTotalNet().add(sumValues.getGoodsValueNet()));
                    totals.setTotalDiscountGross(totals.getTotalDiscountGross().add(sumValues.getDiscountGross()));
                    totals.setTotalDiscountNet(totals.getTotalDiscountNet().add(sumValues.getDiscountNet()));
                });

        totals.setGrandTotalGross(totals.getGoodsTotalGross().subtract(totals.getTotalDiscountGross()));
        totals.setGrandTotalNet(totals.getGoodsTotalNet().subtract(totals.getTotalDiscountNet()));
        totals.setPaymentTotal(totals.getGrandTotalGross());

        returnLatestJson.getOrderHeader().setTotals(totals);
        return returnLatestJson;
    }

    private Collection<CreditNoteLine> negateCreditNoteLines(Collection<CreditNoteLine> creditNoteLines) {
        return creditNoteLines.stream().map(creditNoteLine ->
                CreditNoteLine.builder()
                        .isShippingCost(creditNoteLine.getIsShippingCost())
                        .itemNumber(creditNoteLine.getItemNumber())
                        .quantity(creditNoteLine.getQuantity().negate())
                        .lineTaxAmount(creditNoteLine.getLineTaxAmount())
                        .lineNetAmount(creditNoteLine.getLineNetAmount().negate())
                        .build()
        ).collect(Collectors.toList());
    }

    private void updateShippingCosts(SalesOrderReturn salesOrderReturn, Collection<CreditNoteLine> creditNoteLines) {
        var totals = salesOrderReturn.getReturnOrderJson().getOrderHeader().getTotals();
        creditNoteLines.stream()
                .filter(CreditNoteLine::getIsShippingCost)
                .findFirst()
                .ifPresent(creditNoteLine -> {
                    totals.setShippingCostNet(creditNoteLine.getLineNetAmount());
                    totals.setShippingCostGross(getGrossValue(creditNoteLine));
                });
    }

    private BigDecimal getGrossValue(CreditNoteLine creditNoteLine) {
        var taxRate = creditNoteLine.getLineTaxAmount().abs();
        var netValue = creditNoteLine.getLineNetAmount();
        return CalculationUtil.round(CalculationUtil.getGrossValue(netValue, taxRate), HALF_UP);
    }

    private void cancelOrderRow(String orderGroupId, String orderRowId) {

        List<String> orderNumberListByOrderGroupId = salesOrderService.getOrderNumberListByOrderGroupId(orderGroupId, orderRowId);
        for (String orderNumber : orderNumberListByOrderGroupId) {

            if (helper.checkIfOrderRowProcessExists(orderNumber, orderRowId)) {
                correlateMessageForOrderRowCancelCancellation(orderNumber, orderRowId);

            } else {
                log.debug("Sales order row process does not exist for order number {} and order row: {}",
                        orderNumber, orderRowId);
            }

            if (helper.checkIfActiveProcessExists(orderNumber)) {
                removeCancelledOrderRowFromProcessVariables(orderNumber, orderRowId);
            } else {
                log.debug("Sales order process does not exist for order number {}", orderNumber);
            }

            markOrderRowAsCancelled(orderNumber, orderRowId);
            log.info("Order row cancelled for order number: {} and order row: {}", orderNumber, orderRowId);

        }
    }

    @SuppressWarnings("unchecked")
    private void removeCancelledOrderRowFromProcessVariables(String orderNumber, String orderRowId) {
        var processInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();

        var orderRows = (List<String>) runtimeService.getVariable(processInstance.getId(),
                Variables.ORDER_ROWS.getName());
        orderRows.remove(orderRowId);
        runtimeService.setVariable(processInstance.getId(), Variables.ORDER_ROWS.getName(), orderRows);
    }

    private void markOrderRowAsCancelled(String orderNumber, String orderRowId) {

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        final var latestJson = salesOrder.getLatestJson();
        OrderRows cancelledOrderRow = latestJson.getOrderRows().stream()
                .filter(row -> orderRowId.equals(row.getSku())).findFirst()
                .orElseThrow(() -> new NotFoundException(
                        MessageFormat.format("Could not find order row with SKU {0} for order {1}",
                                orderRowId, orderNumber)));
        cancelledOrderRow.setIsCancelled(true);

        recalculateOrder(latestJson, cancelledOrderRow);
        salesOrderService.save(orderUtil.removeCancelledOrderRowsFromLatestJson(salesOrder), Action.ORDER_ROW_CANCELLED);
    }

    private void recalculateOrder(Order latestJson, OrderRows cancelledOrderRow) {

        SumValues sumValues = cancelledOrderRow.getSumValues();
        Totals totals = latestJson.getOrderHeader().getTotals();

        BigDecimal goodsTotalGross = totals.getGoodsTotalGross().subtract(sumValues.getGoodsValueGross());
        BigDecimal goodsTotalNet = totals.getGoodsTotalNet().subtract(sumValues.getGoodsValueNet());
        BigDecimal totalDiscountGross = Optional.ofNullable(totals.getTotalDiscountGross()).orElse(BigDecimal.ZERO)
                .subtract(Optional.ofNullable(sumValues.getDiscountGross()).orElse(BigDecimal.ZERO));
        BigDecimal totalDiscountNet = Optional.ofNullable(totals.getTotalDiscountNet()).orElse(BigDecimal.ZERO)
                .subtract(Optional.ofNullable(sumValues.getDiscountNet()).orElse(BigDecimal.ZERO));
        BigDecimal grandTotalGross = goodsTotalGross.subtract(totalDiscountGross);
        BigDecimal grantTotalNet = goodsTotalNet.subtract(totalDiscountNet);
        BigDecimal cancelledOrderRowTaxValue = sumValues.getGoodsValueGross().subtract(sumValues.getGoodsValueNet());
        totals.getGrandTotalTaxes().stream()
                .filter(tax -> tax.getRate().equals(cancelledOrderRow.getTaxRate())).findFirst()
                .ifPresent(tax -> tax.setValue(tax.getValue().subtract(cancelledOrderRowTaxValue)));

        totals.setGoodsTotalGross(goodsTotalGross);
        totals.setGoodsTotalNet(goodsTotalNet);
        totals.setTotalDiscountGross(totalDiscountGross);
        totals.setTotalDiscountNet(totalDiscountNet);
        totals.setGrandTotalGross(grandTotalGross);
        totals.setGrandTotalNet(grantTotalNet);
        totals.setPaymentTotal(grandTotalGross);
        latestJson.getOrderHeader().setTotals(totals);
    }

    private List<String> getOriginalOrderSkus(String orderNumber) {

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        Order originalOrder = (Order) salesOrder.getOriginalOrder();
        return originalOrder.getOrderRows().stream().map(OrderRows::getSku).collect(Collectors.toList());
    }

    private boolean isOrderFullyCancelled(Order order) {

        Stream<OrderRows> orderRowsStream = order.getOrderRows().stream()
                .filter(orderRow -> helper.isShipped(orderRow.getShippingType()));
        return orderRowsStream.allMatch(OrderRows::getIsCancelled);
    }

    private void correlateMessageForOrderRowCancelCancellation(String orderNumber, String orderRowId) {
        log.info("Starting cancelling order row process for order number: {} and order row: {}", orderNumber, orderRowId);
        runtimeService.createMessageCorrelation(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), orderRowId)
                .correlateWithResultAndVariables(true);
    }

    private void correlateMessageForOrderCancellation(String orderNumber) {
        log.info("Starting cancelling order process for order number: {}", orderNumber);
        runtimeService.createMessageCorrelation(Messages.ORDER_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .correlateWithResult();
    }

    public void publishOrderRowMsg(RowMessages rowMessage,
                                   String orderGroupId,
                                   String orderItemSku,
                                   String logMessage,
                                   String rawMessage) {
        salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(orderGroupId, orderItemSku).forEach(orderNumber -> {
            try {
                MessageCorrelationResult result = helper.createOrderRowProcess(rowMessage, orderNumber, orderGroupId, orderItemSku);

                if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                    log.info("{} message for order-number {} and sku {} successfully received",
                            logMessage,
                            orderNumber,
                            orderItemSku
                    );
                }
            } catch (Exception e) {
                log.error("{} message error: \r\nOrderNumber: {}\r\nOrderItem-SKU: {}\r\nSQS-Message: {}\r\nError-Message: {}",
                        logMessage,
                        orderNumber,
                        orderItemSku,
                        rawMessage,
                        e.getMessage()
                );
                throw e;
            }
        });
    }
}
