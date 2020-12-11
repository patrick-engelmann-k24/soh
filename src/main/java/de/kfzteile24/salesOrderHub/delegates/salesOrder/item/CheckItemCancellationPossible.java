package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static java.lang.String.format;


@Component
@Log
public class CheckItemCancellationPossible extends CommonDelegate {


    /**
     * Check if process (item) cancellation is possible
     *
     * @param delegateExecution
     */
    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(SHIPMENT_METHOD.getName());
        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case REGULAR:
            case EXPRESS:
                setResultVariable(delegateExecution, checkOnShipmentMethodParcel(delegateExecution));
                return;
            case CLICK_COLLECT:
                setResultVariable(delegateExecution, checkOnShipmentMethodPickup(delegateExecution));
                return;
            case OWN_DELIVERY:
                setResultVariable(delegateExecution, checkOnShipmentMethodOwnDelivery(delegateExecution));
                return;
            default:
                log.warning(format("Unknown Shipment method %s", SHIPMENT_METHOD.getName()));
        }

        // for unknown shipments, we disable cancellation
        setResultVariable(delegateExecution, false);
    }

    protected boolean checkOnShipmentMethodParcel(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), ItemEvents.TRACKING_ID_RECEIVED.getName());
    }

    protected boolean checkOnShipmentMethodPickup(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), ItemEvents.ITEM_PICKED_UP.getName());
    }

    protected boolean checkOnShipmentMethodOwnDelivery(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), ItemEvents.ITEM_DELIVERED.getName());
    }

    void setResultVariable(DelegateExecution delegateExecution, boolean checkResult) {
        setResultVariable(delegateExecution, ItemVariables.ITEM_CANCELLATION_POSSIBLE, checkResult);
    }
}
