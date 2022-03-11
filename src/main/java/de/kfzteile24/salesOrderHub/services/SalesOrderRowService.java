package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationItem;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationMessage;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

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

    @SneakyThrows
    @Transactional
    public void cancelOrderRows(CoreCancellationMessage coreCancellationMessage) {

        String orderNumber = coreCancellationMessage.getOrderNumber();
        for (CoreCancellationItem coreCancellationItem : coreCancellationMessage.getItems()) {
            List<String> originalOrderSkus = getOriginalOrderSkus(orderNumber);
            String skuToCancel = coreCancellationItem.getSku();
            if (originalOrderSkus.contains(skuToCancel)) {
                cancelOrderRow(orderNumber, skuToCancel);
            } else {
                log.error("Sku: {} is not in original order with order number: {}", skuToCancel, orderNumber);
            }
        }
    }

    public void cancelOrderProcessIfFullyCancelled(SalesOrder salesOrder) {

        if (isOrderFullyCancelled(salesOrder.getLatestJson())) {
            cancelOrderProcess(salesOrder);
        }
    }

    public void cancelOrderRow(String orderNumber, String orderRowId) {

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

    public void cancelOrderProcess(SalesOrder salesOrder) {

        log.info("Order with order number: {} is fully cancelled, cancelling the order process", salesOrder.getOrderNumber());
        for (OrderRows orderRow : salesOrder.getLatestJson().getOrderRows()) {
            if (!helper.isShipped(orderRow.getShippingType())) {
                orderRow.setIsCancelled(true);
            }
        }
        salesOrderService.save(salesOrder, Action.ORDER_CANCELLED);
        correlateMessageForOrderCancellation(salesOrder.getOrderNumber());


    }

    public void markOrderRowAsCancelled(String orderNumber, String orderRowId) {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        final var latestJson = salesOrder.getLatestJson();
        latestJson.getOrderRows().stream()
                .filter(row -> orderRowId.equals(row.getSku()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        MessageFormat.format("Could not find order row with SKU {0} for order {1}",
                                orderRowId, orderNumber)))
                .setIsCancelled(true);

        salesOrderService.save(salesOrder, Action.ORDER_ROW_CANCELLED);
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
}
