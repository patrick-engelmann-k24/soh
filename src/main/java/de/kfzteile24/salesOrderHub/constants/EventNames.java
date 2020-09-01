package de.kfzteile24.salesOrderHub.constants;

interface EventNames {
    String EVENT_THROW_MSG_ORDER_PAYMENT_SECURED = "eventThrowMsgOrderPaymentSecured";
    String EVENT_THROW_MSG_ORDER_CREATED = "eventThrowMsgOrderCreated";
    String EVENT_START_SUB_PROCESS_ORDER_ITEM = "eventStartSubProcessOrderItem";
    String EVENT_END_ORDER_ITEM_FINISHED = "eventEndOrderItemFinished";
    String EVENT_CATCH_MSG_ORDER_ITEM_TRANSMITTED_TO_LOGISTICS = "eventCatchMsgOrderItemTransmittedToLogistics";
    String EVENT_CATCH_MSG_ORDER_ITEM_TRACKING_ID_RECEIVED = "eventCatchMsgOrderItemTrackingIdReceived";
    String EVENT_CATCH_MSG_ORDER_ITEM_ITEM_DELIVERED = "eventCatchMsgOrderItemItemDelivered";
    String EVENT_CATCH_MSG_ORDER_ITEM_PACKING_STARTED = "eventCatchMsgOrderItemPackingStarted";
    String EVENT_END_ORDER_ITEM_CANCELLATION_FULLFILMENT_NOT_HANDLED = "eventEndOrderItemCancellationFullfilmentNotHandled";
    String EVENT_START_MSG_ORDER_ITEM_CANCELLATION_RECEIVED = "eventStartMsgOrderItemCancellationReceived";
    String EVENT_THROW_MSG_ORDER_ITEM_FULLFILMENT_CANCELLED = "eventThrowMsgOrderItemFullfilmentCancelled";
    String EVENT_END_ORDER_ITEM_CANCELLATION_FULLFILMENT_HANDLED = "eventEndOrderItemCancellationFullfilmentHandled";
    String EVENT_CATCH_MSG_ORDER_ITEM_TOUR_STARTED = "eventCatchMsgOrderItemTourStarted";
    String EVENT_CATCH_MSG_ORDER_ITEM_PREPARED_FOR_PICKUP = "eventCatchMsgOrderItemPreparedForPickup";
    String EVENT_CATCH_MSG_ORDER_ITEM_PICKED_UP = "eventCatchMsgOrderItemPickedUp";
    String EVENT_START_MSG_ORDER_ITEM_DROPSHIPMENT_CANCELLATION_RECEIVED = "eventStartMsgOrderItemDropshipmentCancellationReceived";
    String EVENT_END_ORDER_ITEM_CANCELLATION_DROPSHIPMENT_NOT_HANDLED = "eventEndOrderItemCancellationDropshipmentNotHandled";
    String EVENT_THROW_ORDER_ITEM_CANCELLATION_DROPSHIPMENT_HANDLED = "eventThrowOrderItemCancellationDropshipmentHandled";
    String EVENT_THROW_MSG_ORDER_ITEM_DROPSHIPMENT_CANCELLED = "eventThrowMsgOrderItemDropshipmentCancelled";
    String EVENT_START_MSG_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE = "eventStartMsgOrderItemDeliveryAddressChange";
    String EVENT_END_ORDER_ITEM_DELIVERY_ADDRESS_NOT_CHANGED = "eventEndOrderItemDeliveryAddressNotChanged";
    String EVENT_THROW_MSG_DELIVERY_ADDRESS_CHANGED = "eventThrowMsgDeliveryAddressChanged";
    String EVENT_CATCH_ESCALATION_ORDER_ITEM_ORDER_CANCELLED = "eventCatchEscalationOrderItemOrderCancelled";
    String EVENT_THROW_MSG_ORDER_ITEM_CANCELLED = "eventThrowMsgOrderItemCancelled";
    String EVENT_START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED = "eventStartMsgInvoiceAddressChangeReceived";
    String EVENT_INVOICE_ADDRESS_NOT_CHANGED = "eventInvoiceAddressNotChanged";
    String EVENT_INVOICE_ADDRESS_CHANGED = "eventInvoiceAddressChanged";
    String EVENT_START_MSG_INVOICE_CREATED = "eventStartMsgInvoiceCreated";
    String EVENT_INVOICE_SAVED = "eventInvoiceSaved";
    String EVENT_THROW_MSG_ORDER_COMPLETED = "eventThrowMsgOrderCompleted";
    String EVENT_START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE = "eventStartMsgOrderReceivedFromMarketplace";
    String EVENT_START_MSG_ORDER_RECEIVED_FROM_E_C_P = "eventStartMsgOrderReceivedFromECP";
    String EVENT_START_MSG_ORDER_RECEIVED_FROM_CUSTOMER_CARE = "eventStartMsgOrderReceivedFromCustomerCare";
    String EVENT_START_MSG_ORDER_RECEIVED_FROM_BRANCH = "eventStartMsgOrderReceivedFromBranch";
    String EVENT_START_MSG_ORDER_RECEIVED_FROM_GARAGE = "eventStartMsgOrderReceivedFromGarage";
    String EVENT_THROW_MSG_ORDER_VALIDATED = "eventThrowMsgOrderValidated";
    String EVENT_END_MSG_ORDER_NOT_VALID_COMPLETED = "eventEndMsgOrderNotValidCompleted";

}
