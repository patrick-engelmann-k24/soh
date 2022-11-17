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
    EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED("eventMsgDropShipmentOrderTrackingInformationReceived"),
    EVENT_MSG_DROPSHIPMENT_INVOICE_ROW_TRACKING_INFORMATION_RECEIVED("eventMsgDropShipmentInvoiceRowTrackingInformationReceived"),
    EVENT_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED("eventMsgDropshipmentOrderRowCancellationReceived"),
    EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLED("eventEndMsgDropshipmentOrderRowCancelled"),
    DROPSHIPMENT_ORDER_ROWS_CANCELLATION("activityDropshipmentOrderRowsCancellation"),
    EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER("eventSignalPauseProcessingDropShipmentOrder"),
    DROPSHIPMENT_ORDER_GENERATE_INVOICE("activityDropShipmentOrderGenerateInvoice"),

    SUB_PROCESS_CORE_SALES_ORDER_CANCELLED("activitySubProcessCoreSalesOrderCancelled"),
    CORE_SALES_ORDER_CANCELLED("activityCoreSalesOrderCancelled"),

    DROPSHIPMENT_INVOICE_ROW_PROCESS("activityDropshipmentInvoiceRowProcess"),
    DROPSHIPMENT_INVOICE_ROW_CREATE_INVOICE_ENTRY("activityCreateInvoiceEntry"),
    DROPSHIPMENT_INVOICE_ROW_PUBLISH_TRACKING_INFORMATION("eventEndMsgPublishTrackingInformation")


    ;

    @Getter
    private final String name;
}
