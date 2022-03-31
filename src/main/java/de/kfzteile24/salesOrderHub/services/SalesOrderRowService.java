package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
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

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;

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
    private final OrderUtil orderUtil;

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
        salesOrder.getLatestJson().getOrderHeader().setOrderNumberExternal(message.getPurchaseOrderNumber());
        salesOrder = salesOrderService.save(salesOrder, DROPSHIPMENT_PURCHASE_ORDER_BOOKED);
        if (!message.getBooked()) {
            for (OrderRows orderRows : ((Order) salesOrder.getOriginalOrder()).getOrderRows())
                cancelOrderRow(orderNumber, orderRows.getSku());
        }
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
