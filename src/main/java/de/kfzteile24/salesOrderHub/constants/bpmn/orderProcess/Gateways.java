package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Gateways implements BpmItem {
    XOR_ORDER_RECEIVED_ECP_OR_MARKETPLACE("gwXOROrderReceivedECPOrMarketplace"),
    XOR_ORDER_VALID("gwXOROrderValid"),
    XOR_INVOICE_EXIST("gwXORInvoiceExist"),
    XOR_CHECK_BRANCH_TYPE("gwXORCheckBranchType"),
    XOR_CHECK_PAYMENT_TYPE("gwXORCheckPaymentType"),
    XOR_CHECK_ORDER_CANCELLATION("gwXORCheckOrderCancellation"),
    XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG("gwXORCheckPauseProcessingDropShipmentOrderFlag"),
    XOR_CHECK_DROPSHIPMENT_ORDER("gwXORCheckDropShipmentOrder"),
    XOR_CHECK_PLATFORM_TYPE("gwXORCheckPlatformType"),
    XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL("gwXORCheckDropShipmentOrderSuccessful"),
    RETURN_ORDER_MAIN_GATEWAY("returnOrderMainGateway"),
    XOR_CHECK_PARTIAL_INVOICE("gwXORIsPartialInvoice"),
    EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE("gwEventCancelOrComplete"),
    XOR_ORDER_FULLY_CANCELLED("gwXOROrderFullyCancelled"),
    ;

    private final String name;

    Gateways(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
