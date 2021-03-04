package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.services.SalesOrderItemService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;


@Component
@Log
public class CheckItemCancellationPossible implements JavaDelegate {

    @Autowired
    private SalesOrderItemService itemService;

    /**
     * Check if process (item) cancellation is possible
     *
     * @param delegateExecution ProcessInstance object
     */
    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(SHIPMENT_METHOD.getName());
        Boolean checkResult = itemService.checkItemCancellationPossible(delegateExecution.getProcessInstanceId(), shipmentMethod);

        delegateExecution.setVariable(ItemVariables.ITEM_CANCELLATION_POSSIBLE.getName(), checkResult);
    }
}
