package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class OrderRowNotFoundException extends NotFoundException {

    public OrderRowNotFoundException(String pattern, Object ... arguments) {
        super(MessageFormat.format(pattern, arguments));
    }

}
