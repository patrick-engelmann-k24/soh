package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

/**
 * @author mykhailo.skliar
 */

public class InvoiceDocumentNotFoundException extends NotFoundException {

    public InvoiceDocumentNotFoundException(String invoiceNumber, String invoiceUrl) {
        super(MessageFormat.format(
                "Invoice Document not found for the given invoice number {0} and this URL: {1}", invoiceNumber, invoiceUrl));
    }

}
