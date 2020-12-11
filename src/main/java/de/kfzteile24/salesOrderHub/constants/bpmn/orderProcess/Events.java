package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Events implements BpmItem {
    CATCH_MSG_ORDER_ITEM_TRANSMITTED_TO_LOGISTICS("eventCatchMsgOrderItemTransmittedToLogistics"),
    CATCH_MSG_ORDER_ITEM_TRACKING_ID_RECEIVED("eventCatchMsgOrderItemTrackingIdReceived"),
    CATCH_MSG_ORDER_ITEM_ITEM_DELIVERED("eventCatchMsgOrderItemItemDelivered"),
    CATCH_MSG_ORDER_ITEM_PACKING_STARTED("eventCatchMsgOrderItemPackingStarted"),
    CATCH_MSG_ORDER_ITEM_TOUR_STARTED("eventCatchMsgOrderItemTourStarted"),
    CATCH_MSG_ORDER_ITEM_PREPARED_FOR_PICKUP("eventCatchMsgOrderItemPreparedForPickup"),
    CATCH_MSG_ORDER_ITEM_PICKED_UP("eventCatchMsgOrderItemPickedUp"),
    CATCH_ESCALATION_ORDER_ITEM_ORDER_CANCELLED("eventCatchEscalationOrderItemOrderCancelled"),

    END_MSG_INVOICE_ADDRESS_CHANGED("eventEndMsgInvoiceAddressChanged"),
    END_MSG_ORDER_CANCELLED("eventEndMsgOrderCancelled"),
    END_MSG_ORDER_COMPLETED("eventEndMsgOrderCompleted"),
    END_MSG_ORDER_NOT_VALID_COMPLETED("eventEndMsgOrderNotValidCompleted"),
    END_ORDER_ITEM_CANCELLATION_FULFILLMENT_NOT_HANDLED("eventEndOrderItemCancellationFullfilmentNotHandled"),
    END_ORDER_ITEM_CANCELLATION_FULFILLMENT_HANDLED("eventEndOrderItemCancellationFullfilmentHandled"),
    END_ORDER_ITEM_DELIVERY_ADDRESS_NOT_CHANGED("eventEndOrderItemDeliveryAddressNotChanged"),
    END_ORDER_ITEM_FINISHED("eventEndOrderItemFinished"),

    INVOICE_ADDRESS_NOT_CHANGED("eventInvoiceAddressNotChanged"),
    INVOICE_SAVED("eventInvoiceSaved"),

    MSG_ORDER_PAYMENT_SECURED("eventMsgOrderPaymentSecured"),

    ORDER_CANCELLATION_RECEIVED("eventOrderCancellationReceived"),

    START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED("eventStartMsgInvoiceAddressChangeReceived"),
    START_MSG_INVOICE_CREATED("eventStartMsgInvoiceCreated"),
    START_MSG_ORDER_ITEM_CANCELLATION_RECEIVED("eventStartMsgOrderItemCancellationReceived"),
    START_MSG_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE("eventStartMsgOrderItemDeliveryAddressChange"),
    START_MSG_ORDER_ITEM_DROPSHIPMENT_CANCELLATION_RECEIVED("eventStartMsgOrderItemDropshipmentCancellationReceived"),
    START_MSG_ORDER_RECEIVED_FROM_BRANCH("eventStartMsgOrderReceivedFromBranch"),
    START_MSG_ORDER_RECEIVED_FROM_CUSTOMER_CARE("eventStartMsgOrderReceivedFromCustomerCare"),
    START_MSG_ORDER_RECEIVED_FROM_ECP("eventStartMsgOrderReceivedFromECP"),
    START_MSG_ORDER_RECEIVED_FROM_GARAGE("eventStartMsgOrderReceivedFromGarage"),
    START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE("eventStartMsgOrderReceivedFromMarketplace"),

    THROW_MSG_ORDER_CREATED("eventThrowMsgOrderCreated"),
    THROW_MSG_ORDER_ITEM_FULLFILMENT_CANCELLED("eventThrowMsgOrderItemFullfilmentCancelled"),
    THROW_MSG_ORDER_ITEM_DROPSHIPMENT_CANCELLED("eventThrowMsgOrderItemDropshipmentCancelled"),
    THROW_MSG_DELIVERY_ADDRESS_CHANGED("eventThrowMsgDeliveryAddressChanged"),
    THROW_MSG_ORDER_ITEM_CANCELLED("eventThrowMsgOrderItemCancelled"),
    THROW_MSG_ORDER_VALIDATED("eventThrowMsgOrderValidated"),
    THROW_ORDER_ITEM_CANCELLATION_DROPSHIPMENT_HANDLED("eventThrowOrderItemCancellationDropshipmentHandled")
    ;

    private final String name;

    Events(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
