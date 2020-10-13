package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum ItemMessages implements BpmItem {
    // process messages,
    MSG_ITEM_TRANSMITTED("msgItemTransmitted"),
    MSG_PACKING_STARTED("msgPackingStarted"),
    MSG_TRACKING_ID_RECEIVED("msgTrackingIdReceived"),
    MSG_ITEM_DELIVERED("msgItemDelivered"),
    MSG_TOUR_STARTED("msgTourStarted"),
    MSG_ITEM_PREPARED("msgItemPrepared"),
    MSG_ITEM_PICKED_UP("msgItemPickedUp"),
    // todo: change name in sales-order-item process
    MSG_DELIVERY_ADDRESS_CHANGE("msg_deliveryAddressChange"),
    MSG_DROPSHIPMENT_CANCELLATION_RECEIVED("msgDropshipmentCancellationReceived"),
    MSG_ORDER_ITEM_CANCELLATION_RECEIVED("msgOrderItemCancellationReceived"),

    EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS("eventStartOrderItemFulfillmentProcess");


    private final String name;

    ItemMessages(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
