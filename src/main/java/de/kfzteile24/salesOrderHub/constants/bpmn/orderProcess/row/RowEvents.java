package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum RowEvents implements BpmItem {
    DELIVERY_ADDRESS_CHANGED("eventDeliveryAddressChanged"),
    DELIVERY_ADDRESS_NOT_CHANGED("eventDeliveryAddressNotChanged"),
    ROW_DELIVERED("eventRowDelivered"),
    ROW_PREPARED_FOR_PICKUP("eventRowPreparedForPickUp"),
    ROW_PICKED_UP("eventRowPickedUp"),
    ROW_TRANSMITTED_TO_LOGISTICS("eventRowTransmittedToLogistics"),
    MSG_DELIVERY_ADDRESS_CHANGE("eventMsgDeliveryAddressChange"),
    MSG_ROW_CANCELLATION_RECEIVED("eventMsgRowCancellationReceived"),
    ORDER_ROW_CANCELLATION_RECEIVED("eventOrderRowCancellationReceived"),
    ORDER_ROW_CANCELLED("eventOrderRowCancelled"),
    //ORDER_ITEM_DROPSHIPMENT_CANCELLED("eventOrderItemDropshipmentCancelled"),
    //ORDER_ITEM_DROPSHIPMENT_NOT_HANDLED("eventOrderItemDropshipmentNotHandled"),
    ORDER_ROW_FULFILLMENT_PROCESS_FINISHED("eventOrderRowFulfillmentProcessFinished"),
    ORDER_ROW_CANCELLATION_HANDLED("eventOrderRowCancellationHandled"),
    ORDER_ROW_CANCELLATION_NOT_HANDLED("eventOrderRowCancellationNotHandled"),
    PACKING_STARTED("eventPackingStarted"),
    START_ORDER_ROW_FULFILLMENT_PROCESS("eventStartOrderRowFulfillmentProcess"),
    TRACKING_ID_RECEIVED("eventTrackingIdReceived"),
    TOUR_STARTED("eventTourStarted"),
    ROW_SHIPPED("rowShipped"),
    ;

    private final String name;

    RowEvents(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
