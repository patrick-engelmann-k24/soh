package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.services.SalesOrderItemService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
@Log
public class CheckItemChangePossible extends CommonDelegate {

    @Autowired
    SalesOrderItemService itemService;

    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(Variables.SHIPMENT_METHOD.getName());
        setResultVariable(delegateExecution,
                itemService.itemChangeable(delegateExecution.getProcessInstanceId(), shipmentMethod));
    }

    void setResultVariable(DelegateExecution delegateExecution, boolean checkResult) {
        setResultVariable(delegateExecution, ItemVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE, checkResult);
    }

}
