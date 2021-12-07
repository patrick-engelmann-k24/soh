package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All available payment types
 */
public enum PaymentType implements BpmItem {
    CREDIT_CARD("creditcard"),
    CASH_ON_DELIVERY("cash_on_delivery"),
    B2B_CASH_ON_DELIVERY("b2b_cash_on_delivery"),
    AFTERPAY_DEBIT("afterpay_debit"),
    AFTERPAY_INVOICE("afterpay_invoice"),
    PAYMENT_IN_ADVANCE("payment_in_advance"),
    B2B_INVOICE("business_to_business_invoice"),
    AMAZON("amazonmarketplace"),
    EBAY("ebay_payment"),
    VOUCHER("voucher");

    private final String name;

    PaymentType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
