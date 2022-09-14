package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

/**
 * @author mykhailo.skliar
 */

public class InvoiceNotFoundException extends NotFoundException {

    public InvoiceNotFoundException(String invoiceNumber) {
        super(MessageFormat.format(
                "Invoice not found for the given invoice number {0} ", invoiceNumber));
    }

}
