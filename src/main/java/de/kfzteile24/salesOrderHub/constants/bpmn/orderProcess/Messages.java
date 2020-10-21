package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Messages implements BpmItem {
    ORDER_RECEIVED_PAYMENT_SECURED("msgOrderReceivedPaymentSecured"),
    ORDER_RECEIVED_MARKETPLACE("msgOrderReceivedMarketplace"),
    ORDER_RECEIVED_ECP("msgOrderReceivedEcp"),
    ORDER_RECEIVED_CUSTOMER_CARE("msgOrderReceivedCustomerCare"),
    ORDER_RECEIVED_BRANCH("msgOrderReceivedBranch"),
    ORDER_RECEIVED_GARAGE("msgOrderReceivedGarage"),
    ORDER_INVOICE_ADDESS_CHANGE_RECEIVED("msg_invoiceAddressChangeReceived"),
    ORDER_CANCELLATION_RECEIVED("msg_orderCancellationReceived")
    ;

    private final String name;

    Messages(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
