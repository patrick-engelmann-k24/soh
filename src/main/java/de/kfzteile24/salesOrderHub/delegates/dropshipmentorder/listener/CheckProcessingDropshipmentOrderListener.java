package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener;

import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckProcessingDropshipmentOrderListener implements ExecutionListener {

    private final KeyValuePropertyService keyValuePropertyService;

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        var pauseDropshipmentProcessing = keyValuePropertyService.getPropertyByKey(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING)
                .orElseThrow(() -> {
                    throw new NotFoundException("Could not find persistent property with key 'pauseDropshipmentProcessing'");
                });

        delegateExecution.setVariable(Variables.PAUSE_DROPSHIPMENT_ORDER_PROCESSING.getName(),
                pauseDropshipmentProcessing.getTypedValue());
    }
}
