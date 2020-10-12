package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum ItemActivities implements BpmItem {
    // activities
    ACTIVITY_CHECK_CANCELLATION_POSSIBLE("activityCheckCancellationPossible"),
    ACTIVITY_HANDLE_CANCELLATION_DROPSHIPMENT("activityHandleCancellationDropshipment"),
    ACTIVITY_HANDLE_CANCELLATION_SHIPMENT("activityHandleCancellationShipment"),
    ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE("activityCheckDeliveryAddressChangePossible"),
    ACTIVITY_CHANGE_DELIVERY_ADDRESS("activityChangeDeliveryAddress"),
    // events
    EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS("eventStartOrderItemFulfillmentProcess"),
    EVENT_ITEM_TRANSMITTED_TO_LOGISTICS("eventItemTransmittedToLogistics"),
    EVENT_PACKING_STARTED("eventPackingStarted"),
    EVENT_MSG_DROPSHIPMENT_CANCELLATION_RECEIVED("eventMsgDropshipmentCancellationReceived"),
    EVENT_MSG_SHIPMENT_CANCELLATION_RECEIVED("eventMsgShipmentCancellationReceived"),
    EVENT_ORDER_CANCEL("eventOrderCancel"),
    EVENT_ORDER_ITEM_CANCELLED("eventOrderItemCancelled"),
    EVENT_ORDER_ITEM_DROPSHIPMENT_CANCELLED("eventOrderItemDropshipmentCancelled"),
    EVENT_ORDER_ITEM_SHIPMENT_CANCELLED("eventOrderItemShipmentCancelled"),
    EVENT_TRACKING_ID_RECEIVED("eventTrackingIdReceived"),
    EVENT_ORDER_ITEM_SHIPMENT_NOT_HANDLED("eventOrderItemShipmentNotHandled"),
    EVENT_ORDER_ITEM_DROPSHIPMENT_NOT_HANDLED("eventOrderItemDropshipmentNotHandled"),

    EVENT_DELIVERY_ADDRESS_CHANGED("eventDeliveryAddressChanged"),
    EVENT_DELIVERY_ADDRESS_NOT_CHANGED("eventDeliveryAddressNotChanged"),
    EVENT_ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED("eventOrderItemFulfillmentProcessFinished"),
    EVENT_ITEM_DELIVERED("eventItemDelivered"),
    EVENT_TOUR_STARTED("eventTourStarted"),
    EVENT_ITEM_PREPARED_FOR_PICKUP("eventItemPreparedForPickUp"),
    EVENT_ITEM_PICKED_UP("eventItemPickedUp"),
    EVENT_MSG_DELIVERY_ADDRESS_CHANGE("eventMsgDeliveryAddressChange"),
    ;

    private final String name;

    ItemActivities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
