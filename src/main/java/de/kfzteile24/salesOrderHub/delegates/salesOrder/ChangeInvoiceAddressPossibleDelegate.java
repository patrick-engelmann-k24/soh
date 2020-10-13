package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Log
public class ChangeInvoiceAddress extends CommonDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        // todo: DEV-15635
        log.info("todo: DEV-15635");
        final boolean hasNotPassed = helper.hasNotPassed(delegateExecution.getProcessInstanceId(), Events.EVENT_INVOICE_SAVED.getName());

        setResultVariable(delegateExecution, Variables.VAR_INVOICE_EXISTS, );
    }
}
