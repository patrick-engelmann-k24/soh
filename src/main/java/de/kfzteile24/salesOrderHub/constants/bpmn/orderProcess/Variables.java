package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    // todo: fix name in process -> orderNumber -> to distinguish better between the id and the "real" orderNumber
    VAR_ORDER_NUMBER("orderId"),
    // todo: fix name in process -> paymentType
    VAR_PAYMENT_TYPE("payment_type"),
    // todo: fix name in process -> paymentStatus
    VAR_PAYMENT_STATUS("payment_status"),
    VAR_ORDER_VALID("orderValid"),
    VAR_ORDER_ITEMS("orderItems"),
    // todo: fix name in process -> shipmentMethod
    VAR_SHIPMENT_METHOD("shipment_method"),
    VAR_INVOICE_EXISTS("invoiceExist");

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
