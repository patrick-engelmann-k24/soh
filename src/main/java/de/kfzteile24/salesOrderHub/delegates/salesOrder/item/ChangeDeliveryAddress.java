package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Log
public class ChangeDeliveryAddress implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        // todo: DEV-15634
        log.info("todo: DEV-15634");
    }
}
