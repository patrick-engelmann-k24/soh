package de.kfzteile24.salesOrderHub.delegates.helper;

import com.amazonaws.util.CollectionUtils;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.exception.NoProcessInstanceFoundException;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_SALES_INVOICE_CREATED_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_FULLY_INVOICED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_GROUP_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PUBLISH_DELAY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_CHANNEL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_ORDER_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.VOUCHER;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

@Component
@RequiredArgsConstructor
@Slf4j
public class CamundaHelper {

    private final HistoryService historyService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;
    private final SleuthHelper sleuthHelper;

    @Value("${kfzteile.process-config.subsequent-order-process.publish-delay}")
    private String publishDelayForSubsequentOrders;
  
    @Value("${kfzteile.orderReturnProcess.publishDelay}")
    private String publishDelay;

    public boolean hasPassed(final String processInstance, final String activityId) {
        var tasks = taskService.createTaskQuery().taskAssigned().list();
        tasks.forEach(task -> taskService.complete(task.getId()));
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

    public ProcessInstance startInvoiceCreatedReceivedProcess(SalesOrder salesOrder) {
        if (salesOrder.getId() == null)
            throw new NotFoundException("Sales order id could not be null");

        Map<String, Object> variables = Map.of(
                ORDER_NUMBER.getName(), salesOrder.getOrderNumber(),
                ORDER_GROUP_ID.getName(), salesOrder.getOrderGroupId(),
                SALES_ORDER_ID.getName(), salesOrder.getId(),
                PUBLISH_DELAY.getName(), publishDelayForSubsequentOrders);

        return runtimeService.createMessageCorrelation(CORE_SALES_INVOICE_CREATED_RECEIVED.getName())
                .processInstanceBusinessKey(salesOrder.getId().toString())
                .setVariables(variables)
                .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance correlateDropshipmentOrderCancelledMessage(String orderNumber) {
        return runtimeService.createMessageCorrelation(DROPSHIPMENT_ORDER_CANCELLED.getName())
                .processInstanceBusinessKey(orderNumber)
                .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance correlateDropshipmentOrderRowShipmentConfirmedMessage(SalesOrder salesOrder, String sku, List<String> trackingLinks) {
        Map<String, Object> variables = Map.of(
                ORDER_NUMBER.getName(), salesOrder.getOrderNumber(),
                ORDER_ROW.getName(), sku,
                TRACKING_LINKS.getName(), trackingLinks);

        return runtimeService.createMessageCorrelation(DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName())
                .processInstanceBusinessKey(salesOrder.getOrderNumber() + "#" + sku)
                .setVariables(variables)
                .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance correlateDropshipmentOrderFullyInvoicedMessage(String orderNumber) {
        return runtimeService.createMessageCorrelation(DROPSHIPMENT_ORDER_FULLY_INVOICED.getName())
                .processInstanceBusinessKey(orderNumber)
                .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance createOrderProcess(SalesOrder salesOrder, Messages originChannel) {
        sleuthHelper.updateTraceId(salesOrder.getOrderNumber());

        return runtimeService.createMessageCorrelation(originChannel.getName())
                             .processInstanceBusinessKey(salesOrder.getOrderNumber())
                             .setVariables(createProcessVariables(salesOrder))
                             .correlateWithResult().getProcessInstance();
    }

    public ProcessInstance createOrderProcess(SalesOrder salesOrder, Messages originChannel, Map<String, Object> variables) {
        sleuthHelper.updateTraceId(salesOrder.getOrderNumber());
        return runtimeService.createMessageCorrelation(originChannel.getName())
                .processInstanceBusinessKey(salesOrder.getOrderNumber())
                .setVariables(variables)
                .correlateWithResult().getProcessInstance();
    }

    @EnrichMessageForDlq
    public void handleInvoiceFromCore(String invoiceUrl, MessageWrapper messageWrapper) {
        final var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);

        log.info("Received invoice from core with order number: {} ", orderNumber);

        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), orderNumber,
                INVOICE_URL.getName(), invoiceUrl
        );
        runtimeService.startProcessInstanceByMessage(Messages.INVOICE_CREATED.getName(), orderNumber, processVariables);
        log.info("Invoice {} from core for order-number {} successfully received", invoiceUrl, orderNumber);
    }

    @EnrichMessageForDlq
    public void handleCreditNoteFromDropshipmentOrderReturn(String invoiceUrl, MessageWrapper messageWrapper) {

        final var returnOrderNumber = InvoiceUrlExtractor.extractReturnOrderNumber(invoiceUrl);
        log.info("Received credit note from dropshipment with return order number: {} ", returnOrderNumber);

        String message = Messages.DROPSHIPMENT_CREDIT_NOTE_DOCUMENT_GENERATED.getName();
        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), returnOrderNumber,
                INVOICE_URL.getName(), invoiceUrl
        );

        runtimeService.startProcessInstanceByMessage(message, returnOrderNumber, processVariables);
        log.info("Invoice {} for credit note of dropshipment order return for order-number {} successfully received",
                invoiceUrl, returnOrderNumber);
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

    public Map<String, Object> createProcessVariables(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();

        List<String> orderRowSkus;
        List<String> virtualOrderRowSkus = new ArrayList<>();
        String shippingType;
        String paymentType;
        String platformType;
        orderRowSkus = new ArrayList<>();
        final var order = (Order) salesOrder.getLatestJson();
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
        processVariables.put(IS_ORDER_CANCELLED.getName(), false);
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

    public boolean waitsOnActivityForMessage(String processInstanceId, BpmItem activity, BpmItem message) {
        var result = Objects.nonNull(runtimeService.createEventSubscriptionQuery()
                .eventType(EventType.MESSAGE.name())
                .eventName(message.getName())
                .processInstanceId(processInstanceId)
                .activityId(activity.getName())
                .singleResult());

        if (!result) {
            log.warn("There is no message subscription {} on activity {} for process instance id {}",
                    message, activity, processInstanceId);
        }
        return result;
    }

    public boolean isShipped(String shippingType) {
        return !ShipmentMethod.NONE.getName().equals(shippingType);
    }

    public void correlateMessage(Messages message, SalesOrder salesOrder, VariableMap variableMap) {

        var processInstanceId = salesOrder.getProcessId();
        var salesOrderNumber = salesOrder.getOrderNumber();

        try {
            log.info("Trying correlate message {} for order number {} and process instance id {}", message.getName(), salesOrderNumber, processInstanceId);
            Objects.requireNonNull(processInstanceId, "Process instance id must be not null");
            MessageCorrelationBuilder messageCorrelationBuilder = runtimeService.createMessageCorrelation(message.getName())
                    .processInstanceId(processInstanceId);

            if (!variableMap.isEmpty()) {
                messageCorrelationBuilder.setVariables(variableMap);
                log.info("Process variables:");
                variableMap.forEach((key, value) -> log.info("\r\n{} -> {}", key, value));
            }

            MessageCorrelationResult messageCorrelationResult = messageCorrelationBuilder.correlateWithResult();

            Optional.ofNullable(messageCorrelationResult.getExecution())
                    .map(Execution::getProcessInstanceId)
                    .ifPresentOrElse(instanceId -> log.info("{} message for order number {} and process instance id {} successfully received",
                            message.getName(), salesOrderNumber, instanceId),
                            () -> {
                                log.error("{} message error for order number {}\r\nError message: Could not correlate message for process instance id {}",
                                        message, salesOrderNumber, processInstanceId);
                                throw new MismatchingMessageCorrelationException("Could not correlate message for order number " + salesOrderNumber);
                            });

        } catch (Exception e) {
            log.error("{} message error for order number {}\r\nError message: {}",
                    message.getName(), salesOrderNumber, e.getLocalizedMessage());
            throw e;
        }
    }

    public void correlateMessage(Messages message, SalesOrder salesOrder) {
        correlateMessage(message, salesOrder, Variables.createVariables());
    }

    public void sendSignal(Signals signal) {
        runtimeService.signalEventReceived(signal.getName());
    }

    public void sendSignal(Signals signal, VariableMap variableMap) {
        var executionQuery = runtimeService.createExecutionQuery()
                .signalEventSubscriptionName(signal.getName());

        variableMap.forEach(executionQuery::processVariableValueEquals);

        Optional.of(executionQuery.list())
                .filter(not(CollectionUtils::isNullOrEmpty))
                .ifPresentOrElse(execution -> {
                        try {
                            runtimeService.signalEventReceived(signal.getName(), execution.get(0).getId());
                        } catch (ProcessEngineException ex) {
                            log.error(ex.getLocalizedMessage(), ex);
                            var errorMessage =
                                    String.format("No active process instance based on %s found waiting for signal %s",
                                            variableMap, signal.getName());
                            throw new NoProcessInstanceFoundException(errorMessage);
                        }
                    },
                        () -> {
                            var errorMessage =
                                    String.format("No active process instance based on %s found subscribed to the signal %s",
                                            variableMap, signal.getName());
                            log.error(errorMessage);
                            throw new NoProcessInstanceFoundException(errorMessage);
                        });
    }

    public void deleteProcessInstancesByProcessDefinitionKeyAndVersionLesserThan(
            de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition processDefinition, int version) {

       repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinition.getName())
                .list().stream()
                .filter(pd -> pd.getVersion() < version)
                .map(ProcessDefinition::getId)
                .forEach(this::deleteProcessInstancesByProcessDefinitionId);
    }

    public void deleteProcessInstancesByProcessDefinitionId(String processDefinitionId) {

            runtimeService.createProcessInstanceQuery()
                    .processDefinitionId(processDefinitionId)
                    .list().stream()
                    .map(ProcessInstance::getProcessInstanceId)
                    .forEach(this::deleteProcessInstance);

    }

    private void deleteProcessInstance(String processInstanceId) {
        try {
            runtimeService.deleteProcessInstanceIfExists(processInstanceId, "MIGRATION", true, false, true, false);
            log.info("Process instance deleted successfully. ID: {}", processInstanceId);
        } catch (Exception ex) {
            log.error("An error occurred while deleting process instance. ID: {}. Error: {}", processInstanceId, ex.getLocalizedMessage());
        }
    }


    public void startDropshipmentSubsequentOrderCreatedProcess(SalesOrder subsequentOrder) {
        String orderNumber = subsequentOrder.getOrderNumber();
        ProcessInstance result = runtimeService.startProcessInstanceByMessage(
                Messages.DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED.getName(),
                orderNumber,
                Map.of(ORDER_NUMBER.getName(), orderNumber));
        if (result != null) {
            log.info("The process of Dropshipment Subsequent Order Created for order number {} successfully started",
                    orderNumber);
        } else {
            log.error("The process of Dropshipment Subsequent Order Created for order number {} could not be started",
                    orderNumber);
        }
    }

    public void setVariable(String processInstanceId, String variableName, Object variableValue) {
        runtimeService.setVariable(processInstanceId, variableName, variableValue);
    }
}
