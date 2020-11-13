package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.lang.String.format;

@Service
@Log
public class SalesOrderItemService {

    @Autowired
    CamundaHelper helper;

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
