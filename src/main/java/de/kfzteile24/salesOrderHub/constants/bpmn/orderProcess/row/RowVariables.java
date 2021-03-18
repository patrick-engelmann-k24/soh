package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum RowVariables implements BpmItem {
    // process variables
    DELIVERY_ADDRESS_CHANGE_POSSIBLE("deliveryAddressChangePossible"),
    // new delivery address, if change is possible
    DELIVERY_ADDRESS_CHANGE_REQUEST("deliveryAddressChangeRequest"),
    ROW_CANCELLED("rowCancelled"),
    ROW_CANCELLATION_POSSIBLE("rowCancellationPossible"),
    ROW_DELIVERED("rowDelivered"),
    ROW_PICKED_UP("rowPickedUp"),
    ORDER_ROW_ID("orderRowId"),
    TRACKING_ID_RECEIVED("trackingIdReceived"),
    ;

    private final String name;

    RowVariables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
