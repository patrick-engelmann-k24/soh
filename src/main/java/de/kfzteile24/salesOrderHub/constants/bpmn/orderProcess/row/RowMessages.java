package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fulfillment process
 */
public enum RowMessages implements BpmItem {
    // process messages,
    ROW_TRANSMITTED_TO_LOGISTICS("msgRowTransmittedToLogistics"),
    PACKING_STARTED("msgPackingStarted"),
    TRACKING_ID_RECEIVED("msgTrackingIdReceived"),
    //ROW_DELIVERED("msgItemDelivered"),
    TOUR_STARTED("msgTourStarted"),
    ROW_PREPARED("msgRowPrepared"),
    ROW_PICKED_UP("msgRowPickedUp"),
    DELIVERY_ADDRESS_CHANGE("msgDeliveryAddressChange"),
    ORDER_ROW_CANCELLATION_RECEIVED("msgOrderRowCancellationReceived"),
    ROW_SHIPPED("msgRowShipped"),
    ;

    private final String name;

    RowMessages(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
