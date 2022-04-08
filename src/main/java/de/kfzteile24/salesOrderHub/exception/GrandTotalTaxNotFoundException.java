package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class GrandTotalTaxNotFoundException extends NotFoundException {

    public GrandTotalTaxNotFoundException(String pattern, Object ... arguments) {
        super(MessageFormat.format(pattern, arguments));
    }

}
