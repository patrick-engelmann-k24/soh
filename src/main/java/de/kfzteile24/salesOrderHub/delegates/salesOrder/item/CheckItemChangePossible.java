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
        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case REGULAR:
            case EXPRESS:
                setResultVariable(delegateExecution, checkOnShipmentMethodParcel(delegateExecution));
                return;
            case CLICK_COLLECT:
                setResultVariable(delegateExecution, false);
                return;
            case OWN_DELIVERY:
                setResultVariable(delegateExecution, checkOnShipmentMethodOwnDelivery(delegateExecution));
                return;
            default:
                log.warning(format("Unknown Shipment method %s", Variables.SHIPMENT_METHOD.getName()));
        }

        // for unknown shipments, we disable cancellation
        setResultVariable(delegateExecution, false);
    }

    protected boolean checkOnShipmentMethodParcel(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), ItemEvents.PACKING_STARTED.getName());
    }

    protected boolean checkOnShipmentMethodOwnDelivery(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), ItemEvents.TOUR_STARTED.getName());
    }

    void setResultVariable(DelegateExecution delegateExecution, boolean checkResult) {
        setResultVariable(delegateExecution, ItemVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE, checkResult);
    }

}
