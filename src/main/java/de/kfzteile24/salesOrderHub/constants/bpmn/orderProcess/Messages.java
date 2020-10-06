package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Messages implements BpmItem {
    // messages
    MSG_ORDER_RECEIVED_PAYMENT_SECURED( "msgOrderReceivedPaymentSecured"),
    MSG_ORDER_RECEIVED_MARKETPLACE ("msgOrderReceivedMarketplace"),
    MSG_ORDER_RECEIVED_ECP ("msgOrderReceivedEcp"),
    MSG_ORDER_RECEIVED_CUSTOMER_CARE ("msgOrderReceivedCustomerCare"),
    MSG_ORDER_RECEIVED_BRANCH("msgOrderReceivedBranch"),
    MSG_ORDER_RECEIVED_GARAGE("msgOrderReceivedGarage"),

    EVENT_THROW_MSG_ORDER_CREATED("eventThrowMsgOrderCreated"),
    EVENT_END_MSG_ORDER_CANCELLED("eventEndMsgOrderCancelled"),

    ;

    private final String name;

    Messages(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
