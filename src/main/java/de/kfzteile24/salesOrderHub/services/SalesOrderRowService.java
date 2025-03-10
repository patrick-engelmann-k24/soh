package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

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
    private final SnsPublishService snsPublishService;

    @Transactional
    public void handleCancellationForOrderRows(String orderNumber, List<OrderRows> orderRows) {

        final var originalSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        var originalOrderRowsNotCancelled = originalSalesOrder.getLatestJson().getOrderRows().stream()
                .filter(not(OrderRows::getIsCancelled))
                .collect(toSet());

        for (OrderRows orderRow : orderRows) {

            var originalSkusToCancel = originalOrderRowsNotCancelled.stream()
                    .filter(not(OrderRows::getIsCancelled))
                    .filter(row -> row.getSku().equals(orderRow.getSku())).collect(Collectors.toList());

            if (!originalSkusToCancel.isEmpty()) {
                BigDecimal sumQuantity = originalSkusToCancel.stream().map(OrderRows::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (orderRow.getQuantity().equals(sumQuantity)) {
                    originalSkusToCancel.forEach(row ->
                            cancelOrderRow(row.getSku(), originalSalesOrder.getOrderNumber()));
                } else {
                    cancelOrderRow(originalSkusToCancel.get(0).getSku(), originalSalesOrder.getOrderNumber());
                }
            }

        }
    }

    public boolean cancelOrderProcessIfFullyCancelled(SalesOrder salesOrder) {
        if (salesOrder.getLatestJson().getOrderRows().stream().allMatch(OrderRows::getIsCancelled)) {
            log.info("Order with order number: {} is fully cancelled", salesOrder.getOrderNumber());
            for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
                if (!ShipmentMethod.isShipped(orderRow.getShippingType())) {
                    orderRow.setIsCancelled(true);
                }
            }
            salesOrder.setCancelled(true);
            salesOrderService.save(salesOrder, Action.ORDER_CANCELLED);
            return true;
        } else {
            return false;
        }
    }

    public void cancelOrderRow(String orderRowId, String orderNumber) {

        markOrderRowAsCancelled(orderNumber, orderRowId);

        if (helper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, orderNumber)) {
            removeCancelledOrderRowFromProcessVariables(orderNumber, orderRowId);
        } else {
            log.debug("Sales order process does not exist for order number {}", orderNumber);
        }

        snsPublishService.publishOrderRowCancelled(orderNumber, orderRowId);
        log.debug("Published Order row cancelled for order number: {} and order row: {}", orderNumber, orderRowId);
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

}
