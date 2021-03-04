package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static java.lang.String.format;

@Service
@Slf4j
public class SalesOrderItemService {

    @Autowired
    CamundaHelper helper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @SneakyThrows
    public ResponseEntity<String> cancelOrderItem(String orderNumber, String orderItemId) {
        if (helper.checkIfItemProcessExists(orderNumber, orderItemId)) {
            sendMessageForOrderItemCancel(orderNumber, orderItemId);

            List<HistoricProcessInstance> queryResult = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName())
                    .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                    .variableValueEquals(ItemVariables.ORDER_ITEM_ID.getName(), orderItemId)
                    .list();

            if (queryResult.size() == 1) {
                HistoricProcessInstance processInstance = queryResult.get(0);
                HistoricVariableInstance variableInstance = historyService.createHistoricVariableInstanceQuery()
                        .processInstanceId(processInstance.getId())
                        .variableName(ItemVariables.ITEM_CANCELLATION_POSSIBLE.getName())
                        .singleResult();
                boolean cancellationPossible = (boolean) variableInstance.getValue();
                if (cancellationPossible) {
                    return ResponseEntity.ok(orderItemId);
                } else {
                    return new ResponseEntity<>("Deletion of item not possible", HttpStatus.CONFLICT);
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
                //setResultVariable(delegateExecution, checkOnShipmentMethodParcel(delegateExecution));
                return helper.hasNotPassed(processId, ItemEvents.TRACKING_ID_RECEIVED.getName());
            case CLICK_COLLECT:
                //setResultVariable(delegateExecution, checkOnShipmentMethodPickup(delegateExecution));
                return helper.hasNotPassed(processId, ItemEvents.ITEM_PICKED_UP.getName());
            case OWN_DELIVERY:
                //setResultVariable(delegateExecution, checkOnShipmentMethodOwnDelivery(delegateExecution));
                return helper.hasNotPassed(processId, ItemEvents.ITEM_DELIVERED.getName());
            default:
                log.warn(format("Unknown Shipment method %s", SHIPMENT_METHOD.getName()));
        }

        return false;
    }

    protected MessageCorrelationResult sendMessageForOrderItemCancel(String orderNumber, String orderItemId) {
        return runtimeService.createMessageCorrelation(ItemMessages.ORDER_ITEM_CANCELLATION_RECEIVED.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(ItemVariables.ORDER_ITEM_ID.getName(), orderItemId)
                .correlateWithResultAndVariables(true);
    }
}
