package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
@Log
@SuppressWarnings("PMD.MissingBreakInSwitch")
public class CheckRowDeliveryAddressChangePossible extends CommonDelegate {

    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(Variables.SHIPMENT_METHOD.getName());
        setResultVariable(delegateExecution,
                itemChangeable(delegateExecution.getProcessInstanceId(), shipmentMethod));
    }

    private void setResultVariable(DelegateExecution delegateExecution, boolean checkResult) {
        setResultVariable(delegateExecution, RowVariables.DELIVERY_ADDRESS_CHANGE_POSSIBLE, checkResult);
    }

    public Boolean itemChangeable(String processInstanceId, String shipmentMethod) {
        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case REGULAR:
            case EXPRESS:
                return checkOnShipmentMethodParcel(processInstanceId);
            case CLICK_COLLECT:
                return false;
            case DIRECT_DELIVERY:
                return checkOnShipmentMethod(processInstanceId);
            default:
                log.warning(format("Unknown Shipment method %s", Variables.SHIPMENT_METHOD.getName()));
        }

        // for unknown shipments, we disable cancellation
        return false;
    }

    protected boolean checkOnShipmentMethodParcel(String processInstanceId) {
        return helper.hasNotPassed(processInstanceId, RowEvents.PACKING_STARTED.getName());
    }

    protected boolean checkOnShipmentMethod(String processInstanceId) {
        return helper.hasNotPassed(processInstanceId, RowEvents.TOUR_STARTED.getName());
    }

}
