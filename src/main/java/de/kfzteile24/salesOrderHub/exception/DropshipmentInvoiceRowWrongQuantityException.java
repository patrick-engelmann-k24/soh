package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class DropshipmentInvoiceRowWrongQuantityException extends RuntimeException {

    public DropshipmentInvoiceRowWrongQuantityException(String sku, String orderNumber) {
        super(MessageFormat.format("Quantity must be more than zero for dropshipment invoice row " +
                "with sku {0} and orderNumber {1}", sku, orderNumber));
    }
}

