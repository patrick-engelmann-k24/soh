package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Log
public class HandleOrderItemCancellation implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) {
        // todo: DEV-15633
        log.info("todo: DEV-15633");
        delegateExecution.setVariable(ItemVariables.ITEM_CANCELLED.getName(), true);
    }
}
