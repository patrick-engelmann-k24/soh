package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.DATE_TIME_FORMATTER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderRowService {

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SalesOrderReturnService salesOrderReturnService;

    @NonNull
    private final OrderUtil orderUtil;

    @NonNull
    private final SnsPublishService snsPublishService;

    public boolean cancelOrderProcessIfFullyCancelled(SalesOrder salesOrder) {

        if (salesOrder.getLatestJson().getOrderRows().stream().allMatch(OrderRows::getIsCancelled)) {
            log.info("Order with order number: {} is fully cancelled", salesOrder.getOrderNumber());
            for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
                if (!helper.isShipped(orderRow.getShippingType())) {
                    orderRow.setIsCancelled(true);
                }
            }
            salesOrderService.save(salesOrder, Action.ORDER_CANCELLED);
            return true;
        } else {
            return false;
        }
    }

    @SneakyThrows
    @Transactional
    public void cancelOrderRows(String orderNumber, List<String> skuList) {

        for (String sku : skuList) {
            List<String> originalOrderSkus = getOriginalOrderSkus(orderNumber);
            if (originalOrderSkus.contains(sku)) {
                cancelOrderRowsOfOrderGroup(orderNumber, sku);
            } else {
                log.error("Sku: {} is not in original order with order number: {}", sku, orderNumber);
            }
        }
    }

    @Transactional
    public void handleSalesOrderReturn(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage, Action action) {
        var salesCreditNoteHeader = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader();
        var orderNumber = salesCreditNoteHeader.getOrderNumber();
        var creditNoteLines = salesCreditNoteHeader.getCreditNoteLines();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        var returnOrderJson = recalculateOrderByReturns(salesOrder, getOrderRowUpdateItems(creditNoteLines));
        updateShippingCosts(returnOrderJson, creditNoteLines);
        returnOrderJson = orderUtil.removeInvalidGrandTotalTaxes(returnOrderJson);

        String newOrderNumber = orderUtil.createOrderNumberInSOH(orderNumber, salesCreditNoteHeader.getCreditNoteNumber());
        returnOrderJson.getOrderHeader().setOrderNumber(newOrderNumber);
        returnOrderJson.getOrderHeader().setOrderDateTime(DATE_TIME_FORMATTER.format(LocalDateTime.now()));

        var salesOrderReturn = SalesOrderReturn.builder()
                .orderGroupId(orderNumber)
                .orderNumber(newOrderNumber)
                .returnOrderJson(returnOrderJson)
                .salesOrder(salesOrder)
                .salesCreditNoteCreatedMessage(updateByOrderNumber(salesCreditNoteCreatedMessage, newOrderNumber))
                .build();

        SalesOrderReturn savedSalesOrderReturn = salesOrderReturnService.save(salesOrderReturn, action);
        ProcessInstance result = helper.createReturnOrderProcess(savedSalesOrderReturn, CORE_CREDIT_NOTE_CREATED);
        if (result != null) {
            log.info("New return order process started for order number: {}. Process-Instance-ID: {} ",
                    orderNumber, result.getProcessInstanceId());
        }
    }

    private SalesCreditNoteCreatedMessage updateByOrderNumber(SalesCreditNoteCreatedMessage message,
                                                              String newOrderNumber) {
        return SalesCreditNoteCreatedMessage.builder()
                .salesCreditNote(SalesCreditNote.builder()
                        .salesCreditNoteHeader(SalesCreditNoteHeader.builder()
                                .orderNumber(newOrderNumber)
                                .orderGroupId(message.getSalesCreditNote().getSalesCreditNoteHeader().getOrderGroupId())
                                .creditNoteNumber(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber())
                                .creditNoteDate(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteDate())
                                .currencyCode(message.getSalesCreditNote().getSalesCreditNoteHeader().getCurrencyCode())
                                .netAmount(message.getSalesCreditNote().getSalesCreditNoteHeader().getNetAmount())
                                .grossAmount(message.getSalesCreditNote().getSalesCreditNoteHeader().getGrossAmount())
                                .billingAddress(message.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress())
                                .creditNoteLines(message.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteLines())
                                .build())
                        .deliveryNotes(message.getSalesCreditNote().getDeliveryNotes())
                        .build())
                .build();
    }

    @Transactional
    public void handleMigrationSubsequentOrder(CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage,
                                               SalesOrder salesOrder) {
        var orderNumber = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var invoiceNumber = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber();
        var newOrderNumber = orderUtil.createOrderNumberInSOH(orderNumber, invoiceNumber);

        if (!salesOrder.getOrderNumber().equals(orderNumber)) {
            var originalSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
            handleMigrationCancellationForOrderRows(
                    originalSalesOrder,
                    salesOrder.getLatestJson().getOrderRows(),
                    invoiceNumber);
            snsPublishService.publishMigrationOrderCreated(newOrderNumber);
            log.info("Invoice with order number {} and invoice number: {} is duplicated with the original sales order. " +
                    "Publishing event on migration topic", orderNumber, invoiceNumber);
        }
        snsPublishService.publishCoreInvoiceReceivedEvent(
                EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        log.info("Publishing migration invoice created event with order number {} and invoice number: {}",
                orderNumber,
                invoiceNumber);
    }

    protected void handleMigrationCancellationForOrderRows(SalesOrder salesOrder,
                                                           List<OrderRows> subsequentOrderRows,
                                                           String invoiceNumber) {
        List<OrderRows> salesOrderRowList = salesOrder.getLatestJson().getOrderRows();
        Set<String> skuList = salesOrderRowList.stream().map(OrderRows::getSku).collect(Collectors.toSet());
        subsequentOrderRows.stream()
                .filter(row -> skuList.contains(row.getSku()))
                .forEach(row -> {
                    log.info("Invoice with invoice number: {} is duplicated with the subsequent sales order. " +
                                    "Publishing event on sales order row cancelled migration topic for order number {} " +
                                    "and order row id {}",
                            invoiceNumber,
                            salesOrder.getOrderNumber(),
                            row.getSku());
                    snsPublishService.publishMigrationOrderRowCancelled(salesOrder.getOrderNumber(), row.getSku());
                });
        if (salesOrderRowList.stream().allMatch(OrderRows::getIsCancelled)) {
            log.info("Invoice with invoice number: {} is duplicated with the subsequent sales order. " +
                            "Publishing event on sales order cancelled migration topic for order number {}",
                    invoiceNumber, salesOrder.getOrderNumber());
            snsPublishService.publishMigrationOrderCancelled(salesOrder.getLatestJson());
        }
    }

    public Order recalculateOrderByReturns(SalesOrder salesOrder, Collection<CreditNoteLine> items) {

        var returnLatestJson = orderUtil.copyOrderJson(salesOrder.getLatestJson());
        returnLatestJson.setOrderRows(new ArrayList<>());
        var totals = returnLatestJson.getOrderHeader().getTotals();
        var lastRowKey = orderUtil.getLastRowKey(salesOrder);

        for (CreditNoteLine item : items) {
            var orderRow = orderUtil.createNewOrderRow(item, salesOrder, lastRowKey);
            returnLatestJson.getOrderRows().add(orderRow);
            lastRowKey = orderUtil.updateLastRowKey(salesOrder, item.getItemNumber(), lastRowKey);
        }

        totals.setShippingCostGross(BigDecimal.ZERO);
        totals.setShippingCostNet(BigDecimal.ZERO);
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
                    totals.setGoodsTotalGross(totals.getGoodsTotalGross().add(
                            Optional.ofNullable(sumValues.getGoodsValueGross()).orElse(BigDecimal.ZERO)));
                    totals.setGoodsTotalNet(totals.getGoodsTotalNet().add(
                            Optional.ofNullable(sumValues.getGoodsValueNet()).orElse(BigDecimal.ZERO)));
                    totals.setTotalDiscountGross(totals.getTotalDiscountGross().add(
                            Optional.ofNullable(sumValues.getDiscountGross()).orElse(BigDecimal.ZERO)));
                    totals.setTotalDiscountNet(totals.getTotalDiscountNet().add(
                            Optional.ofNullable(sumValues.getDiscountNet()).orElse(BigDecimal.ZERO)));
                });

        totals.setGrandTotalGross(totals.getGoodsTotalGross().subtract(
                Optional.ofNullable(totals.getTotalDiscountGross()).orElse(BigDecimal.ZERO)));
        totals.setGrandTotalNet(totals.getGoodsTotalNet().subtract(
                Optional.ofNullable(totals.getTotalDiscountNet()).orElse(BigDecimal.ZERO)));
        totals.setPaymentTotal(totals.getGrandTotalGross());
        totals.setGrandTotalTaxes(salesOrderService.calculateGrandTotalTaxes(returnLatestJson));

        returnLatestJson.getOrderHeader().setTotals(totals);
        return returnLatestJson;
    }

    private List<CreditNoteLine> getOrderRowUpdateItems(Collection<CreditNoteLine> negativedCreditNoteLine) {
        return negativedCreditNoteLine.stream()
                .filter(creditNoteLine -> !creditNoteLine.getIsShippingCost())
                .collect(Collectors.toList());
    }

    private void updateShippingCosts(Order returnOrder, Collection<CreditNoteLine> creditNoteLines) {
        var totals = returnOrder.getOrderHeader().getTotals();
        creditNoteLines.stream()
                .filter(CreditNoteLine::getIsShippingCost)
                .findFirst()
                .ifPresent(creditNoteLine -> {
                    totals.setShippingCostNet(creditNoteLine.getLineNetAmount());
                    totals.setShippingCostGross(creditNoteLine.getUnitGrossAmount());
                    totals.setGrandTotalNet(totals.getGrandTotalNet().add(totals.getShippingCostNet()));
                    totals.setGrandTotalGross(totals.getGrandTotalGross().add(totals.getShippingCostGross()));
                    totals.setPaymentTotal(totals.getGrandTotalGross());
                    BigDecimal fullTaxValue = totals.getGrandTotalGross().subtract(totals.getGrandTotalNet());
                    BigDecimal sumTaxValues = totals.getGrandTotalTaxes().stream()
                            .map(GrandTotalTaxes::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal taxValueToAdd = fullTaxValue.subtract(sumTaxValues);
                    totals.getGrandTotalTaxes().stream().findFirst().
                            ifPresent(tax -> tax.setValue(tax.getValue().add(taxValueToAdd)));
                });
    }

    private void cancelOrderRowsOfOrderGroup(String orderGroupId, String orderRowId) {

        List<String> orderNumberListByOrderGroupId = salesOrderService.getOrderNumberListByOrderGroupId(orderGroupId, orderRowId);
        for (String orderNumber : orderNumberListByOrderGroupId) {
            cancelOrderRow(orderRowId, orderNumber);
        }
    }

    public void cancelOrderRow(String orderRowId, String orderNumber) {

        markOrderRowAsCancelled(orderNumber, orderRowId);

        if (helper.checkIfActiveProcessExists(orderNumber)) {
            removeCancelledOrderRowFromProcessVariables(orderNumber, orderRowId);
        } else {
            log.debug("Sales order process does not exist for order number {}", orderNumber);
        }

        if (helper.checkIfOrderRowProcessExists(orderNumber, orderRowId)) {
            correlateMessageForOrderRowCancelCancellation(orderNumber, orderRowId);

        } else {
            log.debug("Sales order row process does not exist for order number {} and order row: {}",
                    orderNumber, orderRowId);

            snsPublishService.publishOrderRowCancelled(orderNumber, orderRowId);
            log.debug("Published Order row cancelled for order number: {} and order row: {}", orderNumber, orderRowId);
        }
        log.info("Order row cancelled for order number: {} and order row: {}", orderNumber, orderRowId);
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
                .filter(row -> !row.getIsCancelled())
                .filter(row -> orderRowId.equals(row.getSku())).findFirst()
                .orElseThrow(() -> new NotFoundException(
                        MessageFormat.format("Could not find order row with SKU {0} for order {1}",
                                orderRowId, orderNumber)));
        cancelledOrderRow.setIsCancelled(true);

        recalculateOrder(latestJson, cancelledOrderRow);
        salesOrderService.save(salesOrder, Action.ORDER_ROW_CANCELLED);

        boolean isOrderCancelled = cancelOrderProcessIfFullyCancelled(salesOrder);
        log.info("Is order with order number: {} fully check result: {}", orderNumber, isOrderCancelled);
        var processInstance = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();
        if (processInstance != null)
            runtimeService.setVariable(processInstance.getId(), Variables.IS_ORDER_CANCELLED.getName(), isOrderCancelled);
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

    private void correlateMessageForOrderRowCancelCancellation(String orderNumber, String orderRowId) {
        log.info("Starting cancelling order row process for order number: {} and order row: {}", orderNumber, orderRowId);
        List<Execution> processList = runtimeService.createExecutionQuery().
                processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                .processInstanceBusinessKey(orderNumber + "#" + orderRowId)
                .list();
        if (!processList.isEmpty()) {
            var processInstanceId = processList.stream().findFirst().orElseThrow().getProcessInstanceId();
            runtimeService.createMessageCorrelation(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED.getName())
                    .processInstanceBusinessKey(orderNumber + "#" + orderRowId)
                    .processInstanceId(processInstanceId)
                    .correlateWithResult();
        } else {
            log.info("Could not find Order Row process for order number: {} and sku: {}", orderNumber, orderRowId);
        }
    }

    public void correlateOrderRowMessage(RowMessages rowMessage,
                                         String orderGroupId,
                                         String orderItemSku,
                                         String logMessage,
                                         String rawMessage,
                                         RowEvents rowEvents) {
        if (isVirtualSku(orderItemSku)) {
            log.info("sku: {} is a virtual item so it would be ignored for bpmn processes.", orderItemSku);
        } else {
            salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(orderGroupId, orderItemSku).forEach(orderNumber -> {
                try {
                    MessageCorrelationResult result = helper.correlateMessageForOrderRowProcess(rowMessage, orderNumber, rowEvents, orderItemSku);

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

    private boolean isVirtualSku(String sku) {

        return sku.startsWith("KBA") ||
                sku.startsWith("MARK-") ||
                (sku.startsWith("90") && sku.length() == 8 && !sku.contains("-"));
    }
}
