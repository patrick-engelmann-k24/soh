package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class SalesOrderReturnNotFoundException  extends NotFoundException {

    public SalesOrderReturnNotFoundException(String orderNumber) {
        super(MessageFormat.format(
                "Sales order return not found for the given order number {0} ", orderNumber));
    }

}
