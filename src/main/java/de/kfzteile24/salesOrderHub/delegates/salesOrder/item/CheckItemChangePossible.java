package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
@Log
public class CheckItemChangePossible extends CommonDelegate {

    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(Variables.SHIPMENT_METHOD.getName());
        setResultVariable(delegateExecution,
                itemChangeable(delegateExecution.getProcessInstanceId(), shipmentMethod));
    }

    void setResultVariable(DelegateExecution delegateExecution, boolean checkResult) {
        setResultVariable(delegateExecution, ItemVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE, checkResult);
    }

    public Boolean itemChangeable(String processInstanceId, String shipmentMethod) {
        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case REGULAR:
            case EXPRESS:
                return checkOnShipmentMethodParcel(processInstanceId);
            case CLICK_COLLECT:
                return false;
            case OWN_DELIVERY:
                return checkOnShipmentMethodOwnDelivery(processInstanceId);
            default:
                log.warning(format("Unknown Shipment method %s", Variables.SHIPMENT_METHOD.getName()));
        }

        // for unknown shipments, we disable cancellation
        return false;
    }

    protected boolean checkOnShipmentMethodParcel(String processInstanceId) {
        return helper.hasNotPassed(processInstanceId, ItemEvents.PACKING_STARTED.getName());
    }

    protected boolean checkOnShipmentMethodOwnDelivery(String processInstanceId) {
        return helper.hasNotPassed(processInstanceId, ItemEvents.TOUR_STARTED.getName());
    }

}
