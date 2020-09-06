package de.kfzteile24.salesOrderHub.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class HandleCancellation implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        System.out.println("Output hilfe");
        delegateExecution.setVariable("itemCancelled", true);
    }
}
