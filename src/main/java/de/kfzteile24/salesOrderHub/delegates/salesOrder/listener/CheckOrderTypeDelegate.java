package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class CheckOrderTypeDelegate implements ExecutionListener {

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        final String salesChannel = (String) delegateExecution.getVariable(Variables.SALES_CHANNEL.getName());
        boolean branchOrder = salesChannel.contains("branch");
        delegateExecution.setVariable(Variables.IS_BRANCH_ORDER.getName(), branchOrder);
    }
}
