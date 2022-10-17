package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class SalesOrderReturnNotFoundException  extends NotFoundException {

    public SalesOrderReturnNotFoundException(String orderNumber) {
        super(MessageFormat.format(
                "Sales order return not found for the given order number {0} ", orderNumber));
    }

    public SalesOrderReturnNotFoundException(String orderNumber, String creditNoteNumber) {
        super(MessageFormat.format(
                "Sales order return not found for the given order number {0} and credit note number {1} ",
                orderNumber,
                creditNoteNumber));
    }

}
