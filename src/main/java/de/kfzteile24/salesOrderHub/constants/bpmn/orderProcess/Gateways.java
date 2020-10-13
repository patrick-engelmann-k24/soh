package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Gateways implements BpmItem {


    GW_XOR_ORDER_RECEIVED_ECP_OR_MARKETPLACE("gwXOROrderReceivedECPOrMarketplace"),
    GW_XOR_ORDER_VALID("gwXOROrderValid"),
    GW_XOR_ORDER_VALIDATED("gwXOROrderValidated"),
    GW_XOR_CHECK_MANUAL_SUCCESSFUL("gwXORCheckManualSuccessful"),
    GW_XOR_INVOICE_EXIST("gwXORInvoiceExist");

    private final String name;

    Gateways(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
