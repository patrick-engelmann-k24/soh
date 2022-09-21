package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.historyService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.processInstanceQuery;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;

@Component
@Slf4j
@RequiredArgsConstructor
public class BpmUtil {

    @NonNull
    private final RuntimeService runtimeService;

    @NonNull
    private final TimedPollingService pollingService;

    public final List<MessageCorrelationResult> sendMessage(BpmItem message, String orderNumber) {
        return this.sendMessage(message.getName(), orderNumber);
    }

    public final List<MessageCorrelationResult> sendMessage(String message, String orderNumber) {
        return runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals(ORDER_NUMBER.getName(), orderNumber)
                .correlateAllWithResult();
    }

    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderNumber,
                                                      final Map<String, Object> processVariables) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message.getName())
                .processInstanceVariableEquals(ORDER_NUMBER.getName(), orderNumber);
        if (!processVariables.isEmpty())
            builder.setVariables(processVariables);

        return builder
                .correlateWithResult();
    }

    public final String getRandomOrderNumber() {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 8;
        Random random = new Random();
        StringBuilder buffer = new StringBuilder(targetStringLength);
        for (int i = 0; i < targetStringLength; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }

    public final List<String> getOrderRows(final String orderNumber, final int number) {
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            result.add(orderNumber + "-row-" + i);
        }
        return result;
    }

    public boolean isProcessWaitingAtExpectedToken(final ProcessInstance processInstance, final String activityId) {
        return pollingService.pollWithDefaultTiming(() -> {
            assertThat(processInstance).isWaitingAt(activityId);
            return true;
        });
    }

    public void cleanUp() {
        try {
            Optional.of(processInstanceQuery().list().stream()
                    .map(ProcessInstance::getProcessInstanceId)
                    .collect(toUnmodifiableList()))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(pId -> runtimeService().deleteProcessInstancesIfExists(pId, "ANY", true, false, true));

            Optional.of(historyService().createHistoricProcessInstanceQuery().list().stream()
                    .map(HistoricProcessInstance::getId)
                    .collect(toUnmodifiableList()))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(historyService()::deleteHistoricProcessInstancesIfExists);
        } catch (Throwable ignored) {}
    }

    public List<EventSubscription> findEventSubscriptions(EventType eventType, String eventName) {
        return runtimeService.createEventSubscriptionQuery()
                .eventType(eventType.getName())
                .eventName(eventName)
                .list();
    }
}
