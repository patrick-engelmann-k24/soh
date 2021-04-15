package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static java.lang.String.format;

@Service
@Slf4j
public class SalesOrderRowService {

    @Autowired
    CamundaHelper helper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @SneakyThrows
    public ResponseEntity<String> cancelOrderItem(String orderNumber, String orderItemId) {
        if (helper.checkIfItemProcessExists(orderNumber, orderItemId)) {
            sendMessageForOrderRowCancel(orderNumber, orderItemId);

            List<HistoricProcessInstance> queryResult = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                    .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                    .variableValueEquals(RowVariables.ORDER_ROW_ID.getName(), orderItemId)
                    .list();

            if (queryResult.size() == 1) {
                HistoricProcessInstance processInstance = queryResult.get(0);
                HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName(RowVariables.ROW_CANCELLATION_POSSIBLE.getName())
                        .singleResult();
                boolean cancellationPossible = (boolean) variableInstance.getValue();
                if (cancellationPossible) {
                    return ResponseEntity.ok(orderItemId);
                } else {
                    return new ResponseEntity<>("The order row was found, but could not cancelled, because it is already in shipping.", HttpStatus.CONFLICT);
                }
            } else {
                log.debug("More then one instances found " + String.valueOf(queryResult.size()));
            }
            return ResponseEntity.notFound().build();
        } else if (helper.checkIfProcessExists(orderNumber)) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public Boolean checkItemCancellationPossible(String processId, String shipmentMethod) {

        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case REGULAR:
            case EXPRESS:
                return helper.hasNotPassed(processId, RowEvents.TRACKING_ID_RECEIVED.getName());
            case CLICK_COLLECT:
                return helper.hasNotPassed(processId, RowEvents.ROW_PICKED_UP.getName());
            case OWN_DELIVERY:
                return helper.hasNotPassed(processId, RowEvents.ROW_DELIVERED.getName());
            default:
                log.warn(format("Unknown Shipment method %s", SHIPMENT_METHOD.getName()));
        }

        return false;
    }

    protected MessageCorrelationResult sendMessageForOrderRowCancel(String orderNumber, String orderItemId) {
        return runtimeService.createMessageCorrelation(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), orderItemId)
                .correlateWithResultAndVariables(true);
    }
}
