package de.kfzteile24.salesOrderHub.delegates.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_CHANNEL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.VOUCHER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class CamundaHelper {

    @NonNull
    private final HistoryService historyService;

    @NonNull
    private final RuntimeService runtimeService;

    public boolean hasPassed(final String processInstance, final String activityId) {
        final List<HistoricActivityInstance> finishedInstances = historicActivityInstanceQuery(processInstance)
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .orderPartiallyByOccurrence().asc()
                .list();
        final List<HistoricActivityInstance> collect = finishedInstances.parallelStream()
                .filter(e -> e.getActivityId().equals(activityId))
                .collect(toList());
        return collect.size() > 0;
    }

    public boolean hasNotPassed(final String processInstance, final String activityId) {
        return !hasPassed(processInstance, activityId);
    }


    protected HistoricActivityInstanceQuery historicActivityInstanceQuery(final String processInstance) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance);
    }

    public ProcessInstance createOrderProcess(SalesOrder salesOrder, Messages originChannel) {

        return runtimeService.createMessageCorrelation(originChannel.getName())
                             .processInstanceBusinessKey(salesOrder.getOrderNumber())
                             .setVariables(createProcessVariables(salesOrder))
                             .correlateWithResult().getProcessInstance();
    }

    protected Map<String, Object> createProcessVariables(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();

        List<String> orderRowSkus;
        List<String> virtualOrderRowSkus = new ArrayList<>();
        String shippingType;
        String paymentType;
        String platformType;
        orderRowSkus = new ArrayList<>();
        final var order = (Order) salesOrder.getOriginalOrder();
        platformType = order.getOrderHeader().getPlatform().name();
        paymentType = getPaymentType(order.getOrderHeader().getPayments());
        shippingType = order.getOrderRows().get(0).getShippingType();
        for (OrderRows orderRow : order.getOrderRows()) {
            if (isShipped(orderRow.getShippingType())) {
                shippingType = orderRow.getShippingType();
                orderRowSkus.add(orderRow.getSku());
            } else {
                virtualOrderRowSkus.add(orderRow.getSku());
            }
        }


        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(SHIPMENT_METHOD.getName(), shippingType);
        processVariables.put(PLATFORM_TYPE.getName(), platformType);
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(PAYMENT_TYPE.getName(), paymentType);
        processVariables.put(ORDER_ROWS.getName(), orderRowSkus);
        processVariables.put(CUSTOMER_TYPE.getName(),
                salesOrder.isRecurringOrder() ? RECURRING.getType(): NEW.getType());
        processVariables.put(SALES_CHANNEL.getName(), salesOrder.getSalesChannel());

        if(!virtualOrderRowSkus.isEmpty()) {
            processVariables.put(VIRTUAL_ORDER_ROWS.getName(), virtualOrderRowSkus);
        }

        return processVariables;
    }

    private String getPaymentType(List<Payments> payments) {
        return payments.stream()
                .map(Payments::getType)
                .filter(paymentType -> !VOUCHER.getName().equals(paymentType))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Order does not contain a valid payment type"));
    }

    public boolean checkIfOrderRowProcessExists(String orderNumber, String orderRowId) {
        var result = runtimeService.createProcessInstanceQuery()
                                           .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                                           .variableValueEquals(ORDER_NUMBER.getName(), orderNumber)
                                           .variableValueEquals(ORDER_ROW_ID.getName(), orderRowId)
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

    public boolean isShipped(String shippingType) {
        return !ShipmentMethod.NONE.getName().equals(shippingType);
    }
}
