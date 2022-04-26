package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    ORDER_NUMBER("orderNumber"),
    ORDER_GROUP_ID("orderGroupId"),
    PAYMENT_TYPE("paymentType"),
    PAYMENT_STATUS("paymentStatus"),
    PLATFORM_TYPE("platformType"),
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
    CUSTOMER_EMAIL("customerEmail"),
    CUSTOMER_TYPE("customerType"),
    SALES_CHANNEL("salesChannel"),
    IS_BRANCH_ORDER("isBranchOrder"),
    IS_SOH_ORDER("isSohOrder"),
    POSITIVE_PAYMENT_TYPE("positivePaymentType"),
    PUBLISH_DELAY("publishDelay")
    ;

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
