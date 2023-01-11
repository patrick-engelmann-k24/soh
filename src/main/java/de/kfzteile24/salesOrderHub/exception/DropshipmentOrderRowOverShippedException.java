package de.kfzteile24.salesOrderHub.exception;

import java.text.MessageFormat;

public class DropshipmentOrderRowOverShippedException extends RuntimeException {

    public DropshipmentOrderRowOverShippedException(String sku, String orderNumber, Integer newQuantity) {
        super(MessageFormat.format(
                "Dropshipment Order Row with  sku {0} and order number {1} " +
                        "will be over shipped with new quantity {2}", sku, orderNumber, newQuantity));
    }
}
