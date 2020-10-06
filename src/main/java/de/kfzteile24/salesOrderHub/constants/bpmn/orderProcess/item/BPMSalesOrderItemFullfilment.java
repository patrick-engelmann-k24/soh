package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum BPMSalesOrderItemFullfilment implements BpmItem {
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
    EVENT_MSG_DELIVERY_ADDRESS_CHANGE("eventMsgDeliveryAddressChange"),
    EVENT_DELIVERY_ADDRESS_CHANGED("eventDeliveryAddressChanged"),
    EVENT_DELIVERY_ADDRESS_NOT_CHANGED("eventDeliveryAddressNotChanged"),
    EVENT_ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED("eventOrderItemFulfillmentProcessFinished"),
    EVENT_ITEM_DELIVERED("eventItemDelivered"),
    EVENT_TOUR_STARTED("eventTourStarted"),
    EVENT_ITEM_PREPARED_FOR_PICKUP("eventItemPreparedForPickUp"),
    EVENT_ITEM_PICKED_UP("eventItemPickedUp"),
    // Gateways,
    GW_XOR_SHIPMENT_METHOD("gwXORShipmentMethod"),
    GW_XOR_CANCELLATION_POSSIBLE("gwXORCancellationPossible"),
    GW_XOR_DROP_SHIPMENT("gwXORDropShipment"),
    GW_XOR_TRACKING_ID_RECEIVED("gwXORTrackingIdReceived"),
    GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE("gwXORDeliveryAdressChangePossible"),
    GW_XOR_TOUR_STARTED("gwXORTourStarted"),

    SUB_PROCESS_ORDER_ITEM_CANCELLATION_DROPSHIPMENT("subProcessOrderItemCancellationDropshipment"),
    SUB_PROCESS_HANDLE_ORDER_ITEM_CANCELLATION("subProcessHandleOrderItemCancellation"),
    SUB_PROCESS_ORDER_ITEM_CANCELLATION_SHIPMENT("subProcessOrderItemCancellationShipment")
    ;

    private final String name;

    BPMSalesOrderItemFullfilment(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
