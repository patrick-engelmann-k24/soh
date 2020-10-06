package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Log
public class InvoiceAddressChangedDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("ToDo" + delegateExecution.getEventName());
    }

}
