package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import lombok.extern.java.Log;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.BPMSalesOrderItemFullfilment;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
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
        delegateExecution.setVariable(ItemVariables.VAR_ITEM_CANCELLED.getName(), true);
    }
}
