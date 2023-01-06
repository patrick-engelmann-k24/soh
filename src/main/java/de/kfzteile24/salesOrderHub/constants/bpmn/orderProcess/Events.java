package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Events implements BpmItem {
    END_MSG_INVOICE_ADDRESS_CHANGED("eventEndMsgInvoiceAddressChanged"),
    END_MSG_ORDER_CANCELLED("eventEndMsgOrderCancelled"),
    END_MSG_ORDER_COMPLETED("eventEndMsgOrderCompleted"),
    INVOICE_ADDRESS_NOT_CHANGED("eventInvoiceAddressNotChanged"),
    INVOICE_SAVED("eventInvoiceSaved"),
    CREDIT_NOTE_URL_SAVED("eventCreditNoteDocumentGenerated"),
    MSG_ORDER_PAYMENT_SECURED("eventMsgOrderPaymentSecured"),
    START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED("eventStartMsgInvoiceAddressChangeReceived"),
    START_MSG_INVOICE_CREATED("eventStartMsgInvoiceCreated"),
    START_MSG_CREDIT_NOTE_DOCUMENT_GENERATED("eventStartMsgCreditNoteUrlReceived"),
    START_MSG_ORDER_RECEIVED_FROM_BRANCH("eventStartMsgOrderReceivedFromBranch"),
    START_MSG_ORDER_RECEIVED_FROM_CUSTOMER_CARE("eventStartMsgOrderReceivedFromCustomerCare"),
    START_MSG_ORDER_RECEIVED_FROM_ECP("eventStartMsgOrderReceivedFromECP"),
    START_MSG_ORDER_RECEIVED_FROM_GARAGE("eventStartMsgOrderReceivedFromGarage"),
    START_MSG_ORDER_RECEIVED_FROM_MARKETPLACE("eventStartMsgOrderReceivedFromMarketplace"),
    START_MSG_ORDER_CREATED_IN_SOH("eventStartMsgOrderCreatedInSOH"),
    THROW_MSG_ORDER_CREATED("eventThrowMsgOrderCreated"),
    THROW_MSG_ORDER_VALIDATED("eventThrowMsgOrderValidated"),
    THROW_MSG_DROPSHIPMENT_ORDER_CREATED("eventThrowMsgDropshipmentOrderCreated"),
    THROW_MSG_PUBLISH_RETURN_ORDER_CREATED("eventThrowMsgPublishReturnOrderCreated"),
    END_MSG_CORE_CREDIT_NOTE_RECEIVED("eventEndMsgCoreCreditNoteReceived"),
    START_MSG_CORE_CREDIT_NOTE_CREATED("eventStartMsgCoreCreditNoteCreated"),
    START_MSG_DROPSHIPMENT_ORDER_RETURN_CONFIRMED("eventStartMsgDropshipmentOrderReturnConfirmed"),
    MSG_ORDER_CORE_SALES_INVOICE_CREATED("eventMsgOrderCoreSalesInvoiceCreated"),
    START_MSG_CORE_SALES_ORDER_CANCELLED("eventStartMsgCoreSalesOrderCancelled"),
    END_MSG_CORE_SALES_ORDER_CANCELLED("eventEndMsgCoreSalesOrderCancelled"),
    START_MSG_DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED("eventStartMsgDropshipmentShipmentConfirmationReceived"),
    END_MSG_PUBLISH_DROPSHIPMENT_ITEM_SHIPMENT_COMPLETED("eventEndMsgPublishDropshipmentItemShipmentCompleted"),
    MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED("eventMsgDropshipmentOrderFullyCompleted"),
    END_DROPSHIPMENT_SHIPMENT("eventEndDropshipmentShipment")
    ;

    @Getter
    private final String name;
}
