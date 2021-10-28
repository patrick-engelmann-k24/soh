package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("PMD.MissingBreakInSwitch")
public class SalesOrderRowService {

    @NonNull
    private final CamundaHelper helper;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final HistoryService historyService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    public Boolean checkOrderRowCancellationPossible(String processId, String shipmentMethod) {

        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case REGULAR:
            case EXPRESS:
                return helper.hasNotPassed(processId, RowEvents.TRACKING_ID_RECEIVED.getName());
            case CLICK_COLLECT:
                return helper.hasNotPassed(processId, RowEvents.ROW_PICKED_UP.getName());
            case DIRECT_DELIVERY:
                return helper.hasNotPassed(processId, RowEvents.ROW_DELIVERED.getName());
            default:
                log.warn(format("Unknown Shipment method %s", SHIPMENT_METHOD.getName()));
        }

        return false;
    }

    @SneakyThrows
    public ResponseEntity<String> cancelOrderRow(String orderNumber, String orderRowId) {
        if (helper.checkIfOrderRowProcessExists(orderNumber, orderRowId)) {
            sendMessageForOrderRowCancelCancellation(orderNumber, orderRowId);

            List<HistoricProcessInstance> queryResult = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                    .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                    .variableValueEquals(RowVariables.ORDER_ROW_ID.getName(), orderRowId)
                    .list();

            if (queryResult.size() == 1) {
                HistoricProcessInstance processInstance = queryResult.get(0);
                HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName(RowVariables.ROW_CANCELLATION_POSSIBLE.getName())
                        .singleResult();
                boolean cancellationPossible = (boolean) variableInstance.getValue();
                if (cancellationPossible) {
                    return ResponseEntity.ok(orderRowId);
                } else {
                    return new ResponseEntity<>("The order row was found, but could not cancelled, because it is already in shipping.", HttpStatus.CONFLICT);
                }
            } else {
                log.debug("More then one instances found {}", queryResult.size());
            }
            return ResponseEntity.notFound().build();
        } else if (helper.checkIfActiveProcessExists(orderNumber)) {
            cancelOrderRowWithoutSubprocess(orderNumber, orderRowId);

            return ResponseEntity.ok(orderRowId);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public void cancelOrderRowWithoutSubprocess(String orderNumber, String orderRowId) {
        removeCancelledOrderRowFromProcessVariables(orderNumber, orderRowId);

        final var salesOrder = markOrderRowsAsCancelled(orderNumber, orderRowId);
        if (isFullyCancelled(salesOrder.getLatestJson())) {
            salesOrder.getLatestJson().getOrderRows().forEach(orderRow -> {
                if (!helper.isShipped(orderRow.getShippingType())) {
                    orderRow.setIsCancelled(true);
                }
            });
            sendMessageForOrderCancellation(orderNumber);
        } else {
            publishOrderRowsCancelled(orderRowId, salesOrder);
        }
    }

    public void publishOrderRowsCancelled(String orderRowId, SalesOrder salesOrder) {
        final var latestJson = salesOrder.getLatestJson();
        final boolean isFullyCancelled = isFullyCancelled(latestJson);
        final var cancelledRows = latestJson.getOrderRows().stream()
                .filter(orderRow -> orderRow.getSku().equals(orderRowId))
                .collect(toList());

        snsPublishService.publishOrderRowsCancelled(latestJson, cancelledRows, isFullyCancelled);
    }

    public SalesOrder markOrderRowsAsCancelled(String orderNumber, String orderRowId) {
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
        return salesOrder;
    }

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

    public boolean isFullyCancelled(Order order) {
        return order.getOrderRows().stream()
                .filter(orderRow -> helper.isShipped(orderRow.getShippingType()))
                .allMatch(OrderRows::getIsCancelled);
    }

    private MessageCorrelationResult sendMessageForOrderRowCancelCancellation(String orderNumber, String orderRowId) {
        return runtimeService.createMessageCorrelation(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), orderRowId)
                .correlateWithResultAndVariables(true);
    }

    private void sendMessageForOrderCancellation(String orderNumber) {
        runtimeService.createMessageCorrelation(Messages.ORDER_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .correlateWithResult();
    }
}
