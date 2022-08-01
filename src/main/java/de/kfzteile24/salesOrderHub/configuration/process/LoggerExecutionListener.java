package de.kfzteile24.salesOrderHub.configuration.process;

import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.extension.reactor.bus.CamundaSelector;
import org.camunda.bpm.extension.reactor.spring.listener.ReactorExecutionListener;
import org.springframework.stereotype.Component;

import static org.camunda.bpm.engine.delegate.ExecutionListener.EVENTNAME_START;

@CamundaSelector(type = "startEvent", event = EVENTNAME_START)
@RequiredArgsConstructor
@Component
@Slf4j
public class LoggerExecutionListener extends ReactorExecutionListener {

    private final SleuthHelper sleuthHelper;

    @Override
    public void notify(DelegateExecution delegateExecution) {
        sleuthHelper.updateTraceId(delegateExecution.getBusinessKey());
    }
}
