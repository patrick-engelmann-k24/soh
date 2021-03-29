package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    ORDER_NUMBER("orderNumber"),
    PAYMENT_TYPE("paymentType"),
    PAYMENT_STATUS("paymentStatus"),
    ORDER_VALID("orderValid"),
    ORDER_ROWS("orderRows"),
    SHIPMENT_METHOD("shipmentMethod"),
    INVOICE_EXISTS("invoiceExist"),
    INVOICE_ADDRESS_CHANGE_REQUEST("invoiceAddressChangeRequest"),
    ORDER_CANCEL_POSSIBLE("orderCancelPossible"),
    INVOICE_URL("invoiceUrl"),
    ORDER_CANCELED("orderCancelled"),
    CUSTOMER_EMAIL("customerEmail"),
    CUSTOMER_TYPE("customerType")
    ;

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
