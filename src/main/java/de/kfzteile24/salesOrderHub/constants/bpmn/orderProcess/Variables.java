package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    ORDER_NUMBER("orderNumber"),
    PAYMENT_TYPE("paymentType"),
    PAYMENT_STATUS("paymentStatus"),
    ORDER_VALID("orderValid"),
    ORDER_ITEMS("orderItems"),
    SHIPMENT_METHOD("shipmentMethod"),
    INVOICE_EXISTS("invoiceExist"),
    INVOICE_ADDRESS_CHANGE_REQUEST("invoiceAddressChangeRequest"),
    ;

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
