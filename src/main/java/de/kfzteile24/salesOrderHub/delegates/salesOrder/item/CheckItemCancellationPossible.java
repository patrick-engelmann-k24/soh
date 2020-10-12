package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.BPMSalesOrderItemFullfilment;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables.SHIPMENT_METHOD;
import static java.lang.String.format;


@Component
@Log
public class CheckItemCancellationPossible implements JavaDelegate {

    @Autowired
    CamundaHelper helper;

    /**
     * Check if process (item) cancellation is possible
     *
     * @param delegateExecution
     */
    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(SHIPMENT_METHOD.getName());
        switch (ShipmentMethod.fromString(shipmentMethod)) {
            case PARCEL:
                setResultVariable(delegateExecution, checkOnShipmentMethodParcel(delegateExecution));
                return;
            case PICKUP:
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

    protected void setResultVariable(DelegateExecution delegateExecution, boolean checkResult) {
        delegateExecution.setVariable(ItemVariables.VAR_ITEM_CANCELLATION_POSSIBLE.getName(), checkResult);
    }

    protected boolean checkOnShipmentMethodParcel(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), BPMSalesOrderItemFullfilment.EVENT_TRACKING_ID_RECEIVED.getName());
    }

    protected boolean checkOnShipmentMethodPickup(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), BPMSalesOrderItemFullfilment.EVENT_ITEM_PICKED_UP.getName());
    }

    protected boolean checkOnShipmentMethodOwnDelivery(DelegateExecution delegateExecution) {
        return helper.hasNotPassed(delegateExecution.getProcessInstanceId(), BPMSalesOrderItemFullfilment.EVENT_ITEM_DELIVERED.getName());
    }
}
