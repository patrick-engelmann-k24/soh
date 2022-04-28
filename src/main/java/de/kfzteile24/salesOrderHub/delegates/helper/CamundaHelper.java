package de.kfzteile24.salesOrderHub.delegates.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PUBLISH_DELAY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_CHANNEL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_GROUP_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_ORDER_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PUBLISH_DELAY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_INVOICE_CREATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.VOUCHER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
public class CamundaHelper {

    private final HistoryService historyService;
    private final RuntimeService runtimeService;

    @Value("${kfzteile.process-config.subsequent-order-process.publish-delay}")
    private String publishDelayForSubsequentOrders;
  
    @Value("${kfzteile.orderReturnProcess.publishDelay}")
    private String publishDelay;

    public boolean hasPassed(final String processInstance, final String activityId) {
        final List<HistoricActivityInstance> finishedInstances = historicActivityInstanceQuery(processInstance)
                .finished()
                .orderByHistoricActivityInstanceEndTime().asc()
                .orderPartiallyByOccurrence().asc()
                .list();
        final List<HistoricActivityInstance> collect = finishedInstances.parallelStream()
                .filter(e -> e.getActivityId().equals(activityId))
                .collect(toList());
        return !collect.isEmpty();
    }

    public boolean hasNotPassed(final String processInstance, final String activityId) {
        return !hasPassed(processInstance, activityId);
    }

    public MessageCorrelationResult correlateMessageByBusinessKey(BpmItem message, String businessKey) {
        return runtimeService
            .createMessageCorrelation(message.getName())
            .processInstanceBusinessKey(businessKey)
            .correlateWithResult();
    }

    protected HistoricActivityInstanceQuery historicActivityInstanceQuery(final String processInstance) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance);
    }

    public ProcessInstance startInvoiceReceivedProcess(SalesOrder salesOrder) {
        if (salesOrder.getId() == null)
            throw new NotFoundException("Sales order id could not be null");

        Map<String, Object> variables = Map.of(
                ORDER_NUMBER.getName(), salesOrder.getOrderNumber(),
                ORDER_GROUP_ID.getName(), salesOrder.getOrderGroupId(),
                SALES_ORDER_ID.getName(), salesOrder.getId(),
                PUBLISH_DELAY.getName(), publishDelayForSubsequentOrders);

        return runtimeService.createMessageCorrelation(CORE_INVOICE_CREATION_RECEIVED.getName())
                .processInstanceBusinessKey(salesOrder.getId().toString())
                .setVariables(variables)
                .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance createOrderProcess(SalesOrder salesOrder, Messages originChannel) {

        return runtimeService.createMessageCorrelation(originChannel.getName())
                             .processInstanceBusinessKey(salesOrder.getOrderNumber())
                             .setVariables(createProcessVariables(salesOrder))
                             .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance createReturnOrderProcess(SalesOrderReturn salesOrderReturn, Messages originChannel) {

        Map<String, Object> variables = Map.of(
                ORDER_NUMBER.getName(), salesOrderReturn.getOrderNumber(),
                PUBLISH_DELAY.getName(), publishDelay);
        return runtimeService.createMessageCorrelation(originChannel.getName())
                .processInstanceBusinessKey(salesOrderReturn.getOrderNumber())
                .setVariables(variables)
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
                salesOrder.isRecurringOrder() ? RECURRING.getType() : NEW.getType());
        processVariables.put(SALES_CHANNEL.getName(), salesOrder.getSalesChannel());

        if (!virtualOrderRowSkus.isEmpty()) {
            processVariables.put(VIRTUAL_ORDER_ROWS.getName(), virtualOrderRowSkus);
        }

        return processVariables;
    }

    public String getPaymentType(List<Payments> payments) {
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


    /**
     * Send message to bpmn engine
     */
    public MessageCorrelationResult createOrderRowProcess(RowMessages itemMessages,
                                                          String orderNumber,
                                                          String orderGroupId,
                                                          String orderItemSku) {
        return runtimeService.createMessageCorrelation(itemMessages.getName())
                .processInstanceBusinessKey(orderNumber + "#" + orderItemSku)
                .setVariable(ORDER_GROUP_ID.getName(), orderGroupId)
                .setVariable(RowVariables.ORDER_ROW_ID.getName(), orderItemSku)
                .correlateWithResult();
    }
}
