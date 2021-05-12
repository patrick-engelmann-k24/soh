package de.kfzteile24.salesOrderHub.delegates.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;

@Component
@RequiredArgsConstructor
public class CamundaHelper {

    @NonNull
    private final HistoryService historyService;

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final JsonHelper jsonHelper;

    public boolean hasPassed(final String processInstance, final String activityId) {
        final List<HistoricActivityInstance> finishedInstances = historicActivityInstanceQuery(processInstance)
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .orderPartiallyByOccurrence().asc()
                .list();
        final List<HistoricActivityInstance> collect = finishedInstances.parallelStream()
                .filter(e -> e.getActivityId().equals(activityId))
                .collect(Collectors.toList());
        return collect.size() > 0;
    }

    public boolean hasNotPassed(final String processInstance, final String activityId) {
        return !hasPassed(processInstance, activityId);
    }


    protected HistoricActivityInstanceQuery historicActivityInstanceQuery(final String processInstance) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance);
    }

    /**
     *
     * @param salesOrder
     * @param originChannel - from which channel comes the order
     * @return
     */
    public ProcessInstance createOrderProcess(SalesOrder salesOrder, Messages originChannel) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = jsonHelper.getOrderItemsAsStringList(salesOrder.getOriginalOrder());

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(SHIPMENT_METHOD.getName(), salesOrder.getOriginalOrder()
                                                                   .getLogisticalUnits()
                                                                   .get(0)
                                                                   .getShippingType());
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(PAYMENT_TYPE.getName(), salesOrder.getOriginalOrder()
                                                                .getOrderHeader()
                                                                .getPayments()
                                                                .get(0)
                                                                .getType());
        processVariables.put(ORDER_ROWS.getName(), orderItems);
        processVariables.put(CUSTOMER_TYPE.getName(), salesOrder.isRecurringOrder() ?
                                                       RECURRING.getType(): NEW.getType());

        return runtimeService.createMessageCorrelation(originChannel.getName())
                             .processInstanceBusinessKey(orderNumber)
                             .setVariables(processVariables)
                             .correlateWithResult().getProcessInstance();
    }

    public boolean checkIfItemProcessExists(String orderNumber, String orderItemId) {
        var result = runtimeService.createProcessInstanceQuery()
                                           .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                                           .variableValueEquals(ORDER_NUMBER.getName(), orderNumber)
                                           .variableValueEquals(ORDER_ROW_ID.getName(), orderItemId)
                                           .list().isEmpty();

        return !result;
    }

    /**
     * Test if an active process instance for the given order number exists.
     *
     * @param orderNumber the order number to test
     * @return true if an active process instance with the given order number exists false otherwise
     */
    public boolean checkIfActiveProcessExists(String orderNumber) {
        var result = runtimeService.createProcessInstanceQuery()
                                           .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                                           .processInstanceBusinessKey(orderNumber)
                                           .list().isEmpty();
        return !result;
    }

    public Boolean getProcessStatus(Execution execution) {
        return (Boolean) runtimeService.getVariable(execution.getId(), DELIVERY_ADDRESS_CHANGE_POSSIBLE.getName());
    }

    public ProcessInstance getOrderProcess(String processId) {
        return runtimeService.createProcessInstanceQuery()
                             .processInstanceId(processId)
                             .singleResult();
    }

}
