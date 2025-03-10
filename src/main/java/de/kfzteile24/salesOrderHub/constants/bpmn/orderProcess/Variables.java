package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Variables implements BpmItem {
    SALES_ORDER_ID("salesOrderId"),
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
    INVOICE_URL("invoiceUrl"),
    CUSTOMER_TYPE("customerType"),
    SALES_CHANNEL("salesChannel"),
    IS_BRANCH_ORDER("isBranchOrder"),
    IS_SOH_ORDER("isSohOrder"),
    POSITIVE_PAYMENT_TYPE("positivePaymentType"),
    PUBLISH_DELAY("publishDelay"),
    IS_ORDER_CANCELLED("isOrderCancelled"),
    IS_DROPSHIPMENT_ORDER("isDropshipmentOrder"),
    IS_DROPSHIPMENT_ORDER_CONFIRMED("isDropshipmentOrderConfirmed"),
    TRACKING_LINKS("trackingList"),
    ORDER_ROW("orderRow"),
    PAUSE_DROPSHIPMENT_ORDER_PROCESSING("pauseDropshipmentOrderProcessing"),
    IS_DUPLICATE_DROPSHIPMENT_INVOICE("isDuplicateDropshipmentInvoice"),
    INVOICE_NUMBER_LIST("invoiceNumberList"),
    INVOICE_NUMBER("invoiceNumber"),
    IS_PARTIAL_INVOICE("isPartialInvoice"),
    SUBSEQUENT_ORDER_NUMBER("subsequentOrderNumber"),
    ORDER_FULLY_SHIPPED("shipped"),
    ITEMS_FULLY_SHIPPED("itemsFullyShipped"),
    QUANTITY_SHIPPED("quantityShipped"),
    ;

    private final String name;

    Variables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
