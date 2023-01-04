package de.kfzteile24.salesOrderHub.delegates.helper;

import com.amazonaws.util.CollectionUtils;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.NoProcessInstanceFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.event.EventType;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.function.Predicate.not;

@Component
@RequiredArgsConstructor
@Slf4j
public class CamundaHelper {

    private final RuntimeService runtimeService;

    public ProcessInstance startProcessByProcessDefinition(ProcessDefinition processDefinition) {
        return runtimeService.startProcessInstanceByKey(processDefinition.getName());
    }

    public ProcessInstance startProcessByMessage(Messages message, String businessKey,
                                                 Map<String, Object> processVariables) {
        return runtimeService.startProcessInstanceByMessage(message.getName(), businessKey, processVariables);
    }

    public MessageCorrelationResult correlateMessage(Messages message, String businessKey) {
        return runtimeService
            .createMessageCorrelation(message.getName())
            .processInstanceBusinessKey(businessKey)
            .correlateWithResult();
    }

    public MessageCorrelationResult correlateMessage(Messages message, String businessKey,
                                                     Map<String, Object> processVariables) {
        return runtimeService
                .createMessageCorrelation(message.getName())
                .processInstanceBusinessKey(businessKey)
                .setVariables(processVariables)
                .correlateWithResult();
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

    public boolean checkIfActiveProcessExists(ProcessDefinition processDefinition, String businessKey) {
        return !runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinition.getName())
                .processInstanceBusinessKey(businessKey)
                .list()
                .isEmpty();
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

    public void setVariable(String processInstanceId, String variableName, Object variableValue) {
        runtimeService.setVariable(processInstanceId, variableName, variableValue);
    }
}
