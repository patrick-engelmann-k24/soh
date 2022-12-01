package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Activities implements BpmItem {
    CHANGE_INVOICE_ADDRESS("activityChangeInvoiceAddress"),
    CHANGE_INVOICE_ADDRESS_POSSIBLE("activityChangeInvoiceAddressPossible"),
    SUB_PROCESS_INVOICE_ADDRESS_CHANGE("activitySubProcessInvoiceAddressChange"),
    SAVE_INVOICE("activitySaveInvoice"),
    SAVE_CREDIT_NOTE_URL("activitySaveCreditNoteDocumentUrl"),
    EVENT_THROW_MSG_ORDER_CREATED("eventThrowMsgOrderCreated"),
    EVENT_THROW_MSG_PURCHASE_ORDER_CREATED("eventThrowMsgPurchaseOrderCreated"),
    EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED("eventMsgDropShipmentOrderConfirmed"),
    EVENT_END_MSG_DROPSHIPMENT_ORDER_CANCELLED("eventEndMsgDropShipmentOrderCancelled"),
    EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL("eventThrowMsgPurchaseOrderSuccessful"),
    EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED("eventMsgDropshipmentOrderRowShipmentConfirmed"),
    EVENT_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED("eventMsgDropshipmentOrderRowCancellationReceived"),
    EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLED("eventEndMsgDropshipmentOrderRowCancelled"),
    DROPSHIPMENT_ORDER_ROWS_CANCELLATION("activityDropshipmentOrderRowsCancellation"),
    DROPSHIPMENT_ORDER_CANCELLATION("activityDropshipmentOrderCancellation"),
    EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER("eventSignalPauseProcessingDropShipmentOrder"),
    DROPSHIPMENT_ORDER_GENERATE_INVOICE("activityDropShipmentOrderGenerateInvoice"),

    SUB_PROCESS_CORE_SALES_ORDER_CANCELLED("activitySubProcessCoreSalesOrderCancelled"),
    CORE_SALES_ORDER_CANCELLED("activityCoreSalesOrderCancelled"),

    DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS("activityDropshipmentOrderRowShipmentConfirmedProcess"),
    DROPSHIPMENT_ORDER_ROW_CREATE_ENTRY("activityDropshipmentOrderRowCreateEntry"),
    EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_PUBLISH_TRACKING_INFORMATION("eventEndMsgDropshipmentOrderRowPublishTrackingInformation"),
    INVOICING_CREATE_SUBSEQUENT_ORDER("activityCreateInvoiceSubsequentOrder"),
    EVENT_THROW_MSG_START_DROPSHIPMENT_SUBSEQUENT_PROCESS("eventThrowMsgStartDropshipmentSubsequentProcess"),
    CREATE_DROPSHIPMENT_SUBSEQUENT_INVOICE("activityCreateDropshipmentSubsequentInvoice"),
    CALL_ACTIVITY_DROPSHIPMENT_ORDER_ROWS_CANCELLATION("callActivityDropshipmentOrderRowsCancellation"),
    EVENT_THROW_MSG_PUBLISH_PARTLY_INVOICED_DATA("eventThrowMsgPublishPartlyInvoicedData"),
    EVENT_THROW_MSG_GENERATE_PARTLY_INVOICED_PDF("eventThrowMsgGeneratePartlyInvoicedPdf"),
    EVENT_THROW_MSG_CANCEL_DROPSHIPMENT_ORDER("eventThrowMsgCancelDropshipmentOrder"),
    INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE("activityCreateDropshipmentSalesOrderInvoice"),
    EVENT_THROW_MSG_INVOICING_PUBLISH_FULLY_INVOICED_DATA("eventThrowMsgPublishFullyInvoicedData"),
    EVENT_THROW_MSG_INVOICING_DROPSHIPMENT_ORDER_FULLY_INVOICED("eventThrowMsgDropshipmentOrderFullyInvoiced"),
    EVENT_THROW_MSG_INVOICING_GENERATE_FULLY_INVOICED_PDF("eventThrowMsgGenerateFullyInvoicedPdf"),
    EVENT_MSG_DROPSHIPMENT_ORDER_FULLY_INVOICED("eventMsgDropshipmentOrderFullyInvoiced")


    ;

    @Getter
    private final String name;
}
