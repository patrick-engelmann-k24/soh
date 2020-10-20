package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum ItemEvents implements BpmItem {
    DELIVERY_ADDRESS_CHANGED("eventDeliveryAddressChanged"),
    DELIVERY_ADDRESS_NOT_CHANGED("eventDeliveryAddressNotChanged"),
    ITEM_DELIVERED("eventItemDelivered"),
    ITEM_PREPARED_FOR_PICKUP("eventItemPreparedForPickUp"),
    ITEM_PICKED_UP("eventItemPickedUp"),
    ITEM_TRANSMITTED_TO_LOGISTICS("eventItemTransmittedToLogistics"),
    MSG_DELIVERY_ADDRESS_CHANGE("eventMsgDeliveryAddressChange"),
    MSG_SHIPMENT_CANCELLATION_RECEIVED("eventMsgShipmentCancellationReceived"),
    ORDER_ITEM_CANCELLATION_RECEIVED("eventOrderItemCancellationReceived"),
    ORDER_ITEM_CANCELLED("eventOrderItemCancelled"),
    ORDER_ITEM_DROPSHIPMENT_CANCELLED("eventOrderItemDropshipmentCancelled"),
    ORDER_ITEM_DROPSHIPMENT_NOT_HANDLED("eventOrderItemDropshipmentNotHandled"),
    ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED("eventOrderItemFulfillmentProcessFinished"),
    ORDER_ITEM_SHIPMENT_CANCELLED("eventOrderItemShipmentCancelled"),
    ORDER_ITEM_SHIPMENT_NOT_HANDLED("eventOrderItemShipmentNotHandled"),
    PACKING_STARTED("eventPackingStarted"),
    START_ORDER_ITEM_FULFILLMENT_PROCESS("eventStartOrderItemFulfillmentProcess"),
    TRACKING_ID_RECEIVED("eventTrackingIdReceived"),
    TOUR_STARTED("eventTourStarted"),
    ;

    private final String name;

    ItemEvents(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
