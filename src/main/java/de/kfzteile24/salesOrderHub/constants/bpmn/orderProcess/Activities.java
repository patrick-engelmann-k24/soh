package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Activities implements BpmItem {
    CHANGE_INVOICE_ADDRESS("activityChangeInvoiceAddress"),
    CHANGE_INVOICE_ADDRESS_POSSIBLE("activityChangeInvoiceAddressPossible"),
    ORDER_ROW_FULFILLMENT_PROCESS("activityOrderRowFulfillmentProcess"),
    SUB_PROCESS_INVOICE_ADDRESS_CHANGE("activitySubProcessInvoiceAddressChange"),
    ;

    private final String name;

    Activities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
