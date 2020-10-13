package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Activities implements BpmItem {
    // activities
    ACTIVITY_ORDER_ITEM_FULFILLMENT_PROCESS("activityOrderItemFulfillmentProcess"),
    ACTIVITY_CHANGE_INVOICE_ADDRESS("activityChangeInvoiceAddress"),
    ACTIVITY_CHANGE_INVOICE_ADDRESS_POSSIBLE("activityChangeInvoiceAddressPossible"),
    ACTIVITY_SUB_PROCESS_INVOICE_ADDRESS_CHANGE("activitySubProcessInvoiceAddressChange"),
    ;

    private final String name;

    Activities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
