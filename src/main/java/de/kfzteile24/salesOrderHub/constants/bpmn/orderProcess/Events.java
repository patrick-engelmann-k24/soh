package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Events implements BpmItem {
    CATCH_ESCALATION_ORDER_ITEM_ORDER_CANCELLED("eventCatchEscalationOrderItemOrderCancelled"),

    END_MSG_INVOICE_ADDRESS_CHANGED("eventEndMsgInvoiceAddressChanged"),
    END_MSG_ORDER_CANCELLED("eventEndMsgOrderCancelled"),
    END_MSG_ORDER_COMPLETED("eventEndMsgOrderCompleted"),
    END_MSG_ORDER_NOT_VALID_COMPLETED("eventEndMsgOrderNotValidCompleted"),

    INVOICE_ADDRESS_NOT_CHANGED("eventInvoiceAddressNotChanged"),
    INVOICE_SAVED("eventInvoiceSaved"),
    CREDIT_NOTE_SAVED("eventCreditNoteSaved"),

    MSG_ORDER_PAYMENT_SECURED("eventMsgOrderPaymentSecured"),

    ORDER_CANCELLATION_RECEIVED("eventOrderCancellationReceived"),

    START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED("eventStartMsgInvoiceAddressChangeReceived"),
    START_MSG_INVOICE_CREATED("eventStartMsgInvoiceCreated"),
    START_MSG_CREDIT_NOTE_CREATED("eventStartMsgCreditNoteCreated"),
    START_MSG_ORDER_RECEIVED_FROM_BRANCH("eventStartMsgOrderReceivedFromBranch"),
    START_MSG_ORDER_RECEIVED_FROM_CUSTOMER_CARE("eventStartMsgOrderReceivedFromCustomerCare"),
    START_MSG_ORDER_RECEIVED_FROM_ECP("eventStartMsgOrderReceivedFromECP"),
    START_MSG_ORDER_RECEIVED_FROM_GARAGE("eventStartMsgOrderReceivedFromGarage"),
    START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE("eventStartMsgOrderReceivedFromMarketplace"),
    START_MSG_ORDER_CREATED_IN_SOH("eventStartMsgOrderCreatedInSOH"),

    THROW_MSG_ORDER_CREATED("eventThrowMsgOrderCreated"),
    THROW_MSG_DELIVERY_ADDRESS_CHANGED("eventThrowMsgDeliveryAddressChanged"),
    THROW_MSG_ORDER_VALIDATED("eventThrowMsgOrderValidated"),
    THROW_MSG_PUBLISH_TRACKING_INFORMATION("eventThrowMsgPublishTrackingInformation"),
    THROW_MSG_PUBLISH_INVOICE_DATA("eventThrowMsgPublishInvoiceData"),
    THROW_MSG_DROPSHIPMENT_ORDER_CREATED("eventThrowMsgDropshipmentOrderCreated"),

    THROW_MSG_PUBLISH_RETURN_ORDER_CREATED("eventThrowMsgPublishReturnOrderCreated"),
    END_MSG_CORE_CREDIT_NOTE_RECEIVED("eventEndMsgCoreCreditNoteReceived"),
    START_MSG_CORE_CREDIT_NOTE_CREATED("eventStartMsgCoreCreditNoteCreated"),
    START_MSG_DROPSHIPMENT_ORDER_RETURN_CONFIRMED("eventStartMsgDropshipmentOrderReturnConfirmed")
    ;

    @Getter
    private final String name;
}
