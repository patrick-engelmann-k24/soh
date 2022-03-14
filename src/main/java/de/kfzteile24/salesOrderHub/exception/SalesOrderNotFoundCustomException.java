package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class SalesOrderNotFoundCustomException extends NotFoundException {

    public SalesOrderNotFoundCustomException(String description) {
        super(MessageFormat.format(
                "Sales order not found {0} ", description));
    }

}
