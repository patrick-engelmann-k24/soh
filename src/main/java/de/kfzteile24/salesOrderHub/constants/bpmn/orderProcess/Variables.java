package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    VAR_ORDER_NUMBER("orderNumber"),
    VAR_PAYMENT_TYPE("paymentType"),
    VAR_PAYMENT_STATUS("paymentStatus"),
    VAR_ORDER_VALID("orderValid"),
    VAR_ORDER_ITEMS("orderItems"),
    VAR_SHIPMENT_METHOD("shipmentMethod"),
    VAR_INVOICE_EXISTS("invoiceExist");

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
