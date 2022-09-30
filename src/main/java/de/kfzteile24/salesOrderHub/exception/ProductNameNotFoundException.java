package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class ProductNameNotFoundException extends NotFoundException {

    public ProductNameNotFoundException(String pattern, Object ... arguments) {
        super(MessageFormat.format(pattern, arguments));
    }

}