package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ITEM_FULLY_SHIPPED;

@Component
@Slf4j
@RequiredArgsConstructor
public class DropshipmentCreateUpdateShipmentDataDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        delegateExecution.setVariable(ITEM_FULLY_SHIPPED.getName(), false);
    }
}
