package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemSignals;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HandleItemDeletionDelegate implements JavaDelegate {

    @Autowired
    RuntimeService runtimeService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        List<ProcessInstance> orderRowProcesses = runtimeService.createProcessInstanceQuery()
                .superProcessInstanceId(delegateExecution.getProcessInstanceId())
                .list();

        for (ProcessInstance orderRowProcess : orderRowProcesses) {
            runtimeService.createSignalEvent(ItemSignals.SIGNAL_RECEIVE_ORDER_ITEM_CANCELLATION.getName())
                    .executionId(orderRowProcess.getProcessInstanceId())
                    .send();
        }
    }
}
