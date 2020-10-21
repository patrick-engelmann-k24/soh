package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum ItemMessages implements BpmItem {
    // process messages,
    ITEM_TRANSMITTED_TO_LOGISTICS("msgItemTransmittedToLogistics"),
    PACKING_STARTED("msgPackingStarted"),
    TRACKING_ID_RECEIVED("msgTrackingIdReceived"),
    ITEM_DELIVERED("msgItemDelivered"),
    TOUR_STARTED("msgTourStarted"),
    ITEM_PREPARED("msgItemPrepared"),
    ITEM_PICKED_UP("msgItemPickedUp"),
    DELIVERY_ADDRESS_CHANGE("msgDeliveryAddressChange"),
    ORDER_ITEM_CANCELLATION_RECEIVED("msgOrderItemCancellationReceived"),
    ;

    private final String name;

    ItemMessages(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
