package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

/**
 * @author mykhailo.skliar
 */

public class DropshipmentOrderRowNotFoundException extends NotFoundException {

    public DropshipmentOrderRowNotFoundException(String sku, String orderNumber) {
        super(MessageFormat.format(
                "Dropshipment Order Row not found for the given sku {0} and order number {1} ", sku, orderNumber));
    }

}
