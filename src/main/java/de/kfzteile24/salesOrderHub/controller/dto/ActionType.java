package de.kfzteile24.salesOrderHub.controller.dto;

public enum ActionType {

    REPUBLISH_ORDER,
    REPUBLISH_RETURN_ORDER,
    REPUBLISH_ORDER_INVOICE,
    REPUBLISH_CREDIT_NOTE,
    REPUBLISH_DROPSHIPMENT_ORDER,
    RETRIGGER_INVOICE_PDF_GENERATION
}
