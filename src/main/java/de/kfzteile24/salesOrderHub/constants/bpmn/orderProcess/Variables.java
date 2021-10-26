package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    ORDER_NUMBER("orderNumber"),
    PAYMENT_TYPE("paymentType"),
    PAYMENT_STATUS("paymentStatus"),
    ORDER_VALID("orderValid"),
    ORDER_ROWS("orderRows"),
    /** Contains the SKU of each order row having the shipment method 'none'.
     *  {@link de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod#NONE}
     */
    VIRTUAL_ORDER_ROWS("virtualOrderRows"),
    SHIPMENT_METHOD("shipmentMethod"),
    INVOICE_EXISTS("invoiceExist"),
    INVOICE_ADDRESS_CHANGE_REQUEST("invoiceAddressChangeRequest"),
    ORDER_CANCEL_POSSIBLE("orderCancelPossible"),
    INVOICE_URL("invoiceUrl"),
    ORDER_CANCELED("orderCancelled"),
    CUSTOMER_EMAIL("customerEmail"),
    CUSTOMER_TYPE("customerType"),
    SALES_CHANNEL("salesChannel"),
    IS_BRANCH_ORDER("isBranchOrder"),
    POSITIVE_PAYMENT_TYPE("positivePaymentType")
    ;

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
