package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.PACKING_STARTED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.TRACKING_ID_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
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

    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderNumber, final String orderRow,
                                                      final Map<String, Object> processVariables) {
        return sendMessage(message.getName(), orderNumber, orderRow, processVariables);
    }

    public final MessageCorrelationResult sendMessage(final BpmItem message, final String orderNumber, final String orderRow) {
        return sendMessage(message.getName(), orderNumber, orderRow, Collections.emptyMap());
    }

    public final MessageCorrelationResult sendMessage(final String message, final String orderNumber, final String orderRow,
                                                      final Map<String, Object> processVariables) {
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(message)
                .processInstanceVariableEquals(ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(ORDER_ROW_ID.getName(), orderRow);
        if (!processVariables.isEmpty())
            builder.setVariables(processVariables);

        return builder
                .correlateWithResult();
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

    public void finishOrderProcess(final ProcessInstance orderProcess, final String orderNumber) {
        // start subprocess
        sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, orderNumber);

        // send items thru
        sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        sendMessage(PACKING_STARTED, orderNumber);
        sendMessage(TRACKING_ID_RECEIVED, orderNumber);
        sendMessage(ROW_SHIPPED, orderNumber);

        pollingService.pollWithDefaultTiming(() -> {
            assertThat(orderProcess).isEnded().hasPassed(Events.END_MSG_ORDER_COMPLETED.getName());
            return true;
        });
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
