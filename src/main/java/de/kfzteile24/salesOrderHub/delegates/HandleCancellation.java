package de.kfzteile24.salesOrderHub.delegates;

import de.kfzteile24.salesOrderHub.constants.BPMSalesOrderItemFullfilment;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class HandleCancellation implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        delegateExecution.setVariable(BPMSalesOrderItemFullfilment.VAR_ITEM_CANCELLED.getName(), true);
    }
}
