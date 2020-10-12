package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Activities implements BpmItem {
    // activities
    ACTIVITY_VALIDATE_ORDER( "msgOrderReceivedPaymentSecured"),
    ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS("activityOrderItemFulfillmentProcess"),
    ACTIVITY_CHANGE_INVOICE_ADDRESS("activityChangeInvoiceAddress"),
    EVENT_CATCH_MSG_ORDER_CREATED("eventThrowMsgOrderCreated"),
    EVENT_END_MSG_ORDER_CANCELLED("eventEndMsgOrderCancelled"),
    EVENT_END_ORDER_ITEM_FINISHED("eventEndOrderItemFinished"),
    EVENT_CATCH_MSG_ORDER_ITEM_TRANSMITTED_TO_LOGISTICS("eventCatchMsgOrderItemTransmittedToLogistics"),
    EVENT_CATCH_MSG_ORDER_ITEM_TRACKING_ID_RECEIVED("eventCatchMsgOrderItemTrackingIdReceived"),
    EVENT_CATCH_MSG_ORDER_ITEM_ITEM_DELIVERED("eventCatchMsgOrderItemItemDelivered"),
    EVENT_CATCH_MSG_ORDER_ITEM_PACKING_STARTED("eventCatchMsgOrderItemPackingStarted"),
    EVENT_END_ORDER_ITEM_CANCELLATION_FULLFILMENT_NOT_HANDLED("eventEndOrderItemCancellationFullfilmentNotHandled"),
    EVENT_START_MSG_ORDER_ITEM_CANCELLATION_RECEIVED("eventStartMsgOrderItemCancellationReceived"),
    EVENT_THROW_MSG_ORDER_ITEM_FULLFILMENT_CANCELLED("eventThrowMsgOrderItemFullfilmentCancelled"),
    EVENT_END_ORDER_ITEM_CANCELLATION_FULLFILMENT_HANDLED("eventEndOrderItemCancellationFullfilmentHandled"),
    EVENT_CATCH_MSG_ORDER_ITEM_TOUR_STARTED("eventCatchMsgOrderItemTourStarted"),
    EVENT_CATCH_MSG_ORDER_ITEM_PREPARED_FOR_PICKUP("eventCatchMsgOrderItemPreparedForPickup"),
    EVENT_CATCH_MSG_ORDER_ITEM_PICKED_UP("eventCatchMsgOrderItemPickedUp"),
    EVENT_START_MSG_ORDER_ITEM_DROPSHIPMENT_CANCELLATION_RECEIVED("eventStartMsgOrderItemDropshipmentCancellationReceived"),
    EVENT_END_ORDER_ITEM_CANCELLATION_DROPSHIPMENT_NOT_HANDLED("eventEndOrderItemCancellationDropshipmentNotHandled"),
    EVENT_THROW_ORDER_ITEM_CANCELLATION_DROPSHIPMENT_HANDLED("eventThrowOrderItemCancellationDropshipmentHandled"),
    EVENT_THROW_MSG_ORDER_ITEM_DROPSHIPMENT_CANCELLED("eventThrowMsgOrderItemDropshipmentCancelled"),
    EVENT_START_MSG_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE("eventStartMsgOrderItemDeliveryAddressChange"),
    EVENT_END_ORDER_ITEM_DELIVERY_ADDRESS_NOT_CHANGED("eventEndOrderItemDeliveryAddressNotChanged"),
    EVENT_THROW_MSG_DELIVERY_ADDRESS_CHANGED("eventThrowMsgDeliveryAddressChanged"),
    EVENT_CATCH_ESCALATION_ORDER_ITEM_ORDER_CANCELLED("eventCatchEscalationOrderItemOrderCancelled"),
    EVENT_THROW_MSG_ORDER_ITEM_CANCELLED("eventThrowMsgOrderItemCancelled"),
    EVENT_START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED("eventStartMsgInvoiceAddressChangeReceived"),
    EVENT_INVOICE_ADDRESS_NOT_CHANGED("eventInvoiceAddressNotChanged"),
    EVENT_END_MSG_INVOICE_ADDRESS_CHANGED("eventEndMsgInvoiceAddressChanged"),
    EVENT_START_MSG_INVOICE_CREATED("eventStartMsgInvoiceCreated"),
    EVENT_INVOICE_SAVED("eventInvoiceSaved"),
    EVENT_START_MSG_ORDER_RECEIVED_FROM_ECP("eventStartMsgOrderReceivedFromECP"),
    EVENT_START_MSG_ORDER_RECEIVED_FROM_CUSTOMER_CARE("eventStartMsgOrderReceivedFromCustomerCare"),
    EVENT_START_MSG_ORDER_RECEIVED_FROM_BRANCH("eventStartMsgOrderReceivedFromBranch"),
    EVENT_START_MSG_ORDER_RECEIVED_FROM_GARAGE("eventStartMsgOrderReceivedFromGarage"),
    EVENT_END_MSG_ORDER_COMPLETED("eventEndMsgOrderCompleted"),
    EVENT_START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE("eventStartMsgOrderReceivedFromMarketplace"),
    EVENT_THROW_MSG_ORDER_VALIDATED("eventThrowMsgOrderValidated"),
    EVENT_END_MSG_ORDER_NOT_VALID_COMPLETED("eventEndMsgOrderNotValidCompleted"),
    EVENT_MSG_ORDER_PAYMENT_SECURED ("eventMsgOrderPaymentSecured"),
    ;

    private final String name;

    Activities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
