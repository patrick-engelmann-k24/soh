package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

/**
 * @author vinaya
 */

public class SalesOrderNotFoundException extends RuntimeException {

    public SalesOrderNotFoundException(String orderNumber) {
        super(MessageFormat.format(
                "Sales order not found for the given order number {0} ", orderNumber));
    }

}
