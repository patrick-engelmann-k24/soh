package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Messages implements BpmItem {
    ORDER_RECEIVED_PAYMENT_SECURED("msgOrderReceivedPaymentSecured"),
    ORDER_RECEIVED_MARKETPLACE("msgOrderReceivedMarketplace"),
    ORDER_RECEIVED_ECP("msgOrderReceivedEcp"),
    ORDER_RECEIVED_CUSTOMER_CARE("msgOrderReceivedCustomerCare"),
    ORDER_RECEIVED_BRANCH("msgOrderReceivedBranch"),
    ORDER_RECEIVED_GARAGE("msgOrderReceivedGarage"),
    ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED("msgInvoiceAddressChangeReceived"),
    INVOICE_CREATED("msgInvoiceCreated"),
    ORDER_CREATED_IN_SOH("msgOrderCreatedInSoh"),
    CORE_SALES_INVOICE_CREATED_RECEIVED("msgCoreSalesInvoiceCreatedReceived"),
    CORE_CREDIT_NOTE_CREATED("msgCoreCreditNoteCreated"),
    DROPSHIPMENT_ORDER_CONFIRMED("msgDropShipmentOrderConfirmed"),
    DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED("msgDropShipmentOrderTrackingInformationReceived"),
    DROPSHIPMENT_INVOICE_ROW_TRACKING_INFORMATION_RECEIVED("msgDropShipmentInvoiceRowTrackingInformationReceived"),
    DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED("msgDropshipmentOrderRowCancellationReceived"),
    DROPSHIPMENT_CREDIT_NOTE_DOCUMENT_GENERATED("msgCreditNoteDocumentGenerated"),
    DROPSHIPMENT_ORDER_RETURN_CONFIRMED("msgDropshipmentOrderReturnConfirmed"),
    ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED("msgOrderReceivedCoreSalesInvoiceCreated"),
    DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED("msgDropshipmentSubsequentOrderCreated"),
    ;

    @Getter
    private final String name;
}
