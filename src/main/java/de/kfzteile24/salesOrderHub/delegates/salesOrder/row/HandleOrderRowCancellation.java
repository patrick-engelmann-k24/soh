package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Log
public class HandleOrderRowCancellation implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        delegateExecution.setVariable(RowVariables.ROW_CANCELLED.getName(), true);
    }
}
