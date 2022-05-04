package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Gateways implements BpmItem {
    XOR_ORDER_RECEIVED_ECP_OR_MARKETPLACE("gwXOROrderReceivedECPOrMarketplace"),
    XOR_ORDER_VALID("gwXOROrderValid"),
    XOR_ORDER_VALIDATED("gwXOROrderValidated"),
    XOR_CHECK_MANUAL_SUCCESSFUL("gwXORCheckManualSuccessful"),
    XOR_INVOICE_EXIST("gwXORInvoiceExist"),
    XOR_CHECK_BRANCH_TYPE("gwXORCheckBranchType"),
    XOR_CHECK_PAYMENT_TYPE("gwXORCheckPaymentType"),
    XOR_CHECK_ORDER_CANCELLATION("gwXORCheckOrderCancellation")
    ;

    private final String name;

    Gateways(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
