package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener;

import de.kfzteile24.salesOrderHub.constants.CustomEventName;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Extensions;
import de.kfzteile24.salesOrderHub.helper.DateFormatUtil;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.ManagementService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Extensions.NEW_RELIC_EVENT;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Extensions.TEMPORAL_UNIT;
import static java.text.MessageFormat.format;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewRelicAwareTimerListener implements ExecutionListener {

    private final HistoryService historyService;
    private final ManagementService managementService;
    private final MetricsHelper metricsHelper;

    @Override
    public void notify(DelegateExecution delegateExecution) {

        var orderNumber = delegateExecution.getBusinessKey();
        var currentActivityId = delegateExecution.getCurrentActivityId();
        var processInstanceId = delegateExecution.getProcessInstanceId();

        try {

            var currentTimerBoundaryEvent = delegateExecution.getBpmnModelElementInstance().getModelInstance()
                    .getModelElementsByType(BoundaryEvent.class)
                    .stream()
                    .filter(event -> StringUtils.equals(event.getId(), currentActivityId))
                    .findFirst()
                    .orElseThrow();

            var attachedToReceiverTaskActivity = currentTimerBoundaryEvent.getAttachedTo();

            var historicReceiverTaskActivityInstance = historyService.createHistoricActivityInstanceQuery()
                    .activityId(attachedToReceiverTaskActivity.getId())
                    .processInstanceId(processInstanceId)
                    .singleResult();

            var timerJob = managementService.createJobQuery()
                    .timers()
                    .activityId(currentActivityId)
                    .processInstanceId(processInstanceId)
                    .singleResult();

            var camundaProperties = delegateExecution.getBpmnModelElementInstance()
                    .getExtensionElements()
                    .getElementsQuery()
                    .filterByType(CamundaProperties.class)
                    .singleResult()
                    .getCamundaProperties();

            var timerJobDueDate = timerJob.getDuedate();
            var receiverTaskStartTime = historicReceiverTaskActivityInstance.getStartTime();

            getCamundaPropertyValue(camundaProperties, NEW_RELIC_EVENT)
                    .map(CustomEventName::valueOf)
                    .ifPresentOrElse(newRelicEvent -> getCamundaPropertyValue(camundaProperties, TEMPORAL_UNIT)
                                    .map(ChronoUnit::valueOf)
                                    .map(temporalUnit -> createEventAttributes(orderNumber, timerJobDueDate, receiverTaskStartTime, newRelicEvent, temporalUnit))
                                    .ifPresentOrElse(eventAttributeMap -> metricsHelper.sendCustomEvent(newRelicEvent, eventAttributeMap),
                                            () -> log.warn("{} extension not found on timer {}", TEMPORAL_UNIT.getName(), currentActivityId))
                            , () -> log.warn("{} extension not found on timer {}", NEW_RELIC_EVENT.getName(), currentActivityId));
        } catch (Exception e) {
            log.error("Error while executing timer listener {}. {}", currentActivityId, e.getLocalizedMessage());
        }
    }

    private static Map<String, Object> createEventAttributes(String orderNumber, Date timerJobDueDate,
                                                             Date receiverTaskStartTime, CustomEventName newRelicEvent,
                                                             ChronoUnit temporalUnit) {
        var timeJobDueDateLocalDateTime =
                convertToLocalDateTimeAndTruncate(timerJobDueDate, temporalUnit);
        var receiverTaskStartTimeLocalDateTime =
                convertToLocalDateTimeAndTruncate(receiverTaskStartTime, temporalUnit);
        var duration = Duration.between(receiverTaskStartTimeLocalDateTime, timeJobDueDateLocalDateTime);
        var divisor = temporalUnit.getDuration();
        var durationInWholeTemporalUnit = duration.dividedBy(divisor);

        log.info("NewRelic event: {}. Start ts: {}. Timer trigger ts: {}. Duration: {} {}", newRelicEvent.getName(),
                DateFormatUtil.format(receiverTaskStartTimeLocalDateTime),
                DateFormatUtil.format(timeJobDueDateLocalDateTime),
                durationInWholeTemporalUnit, temporalUnit.name().toLowerCase());

        return Map.of(
                "OrderNumber", orderNumber,
                "CreationDate", DateFormatUtil.format(receiverTaskStartTimeLocalDateTime),
                createDurationEventAttributeName(temporalUnit), durationInWholeTemporalUnit
        );
    }

    private static String createDurationEventAttributeName(ChronoUnit temporalUnit) {
        var firstThreeLettersCapitalized = StringUtils.capitalize(temporalUnit.name().toLowerCase().substring(0, 3));
        return format("DurationIn{0}s", firstThreeLettersCapitalized);
    }

    private static Optional<String> getCamundaPropertyValue(Collection<CamundaProperty> camundaProperties,
                                                            Extensions extensionName) {
        return camundaProperties.stream()
                .filter(camundaProperty -> StringUtils.equals(camundaProperty.getCamundaName(), extensionName.getName()))
                .findFirst()
                .map(CamundaProperty::getCamundaValue)
                .map(String.class::cast);
    }

    private static LocalDateTime convertToLocalDateTimeAndTruncate(Date dateToConvert, ChronoUnit chronoUnitTruncateTo) {
        return LocalDateTime.ofInstant(dateToConvert.toInstant(), ZoneId.systemDefault()).truncatedTo(chronoUnitTruncateTo);
    }
}
