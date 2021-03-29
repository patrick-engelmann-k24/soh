package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;


@Component
@Log
@RequiredArgsConstructor
public class InvoiceSavedDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("Invoice has been saved");
    }
}