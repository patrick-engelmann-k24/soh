package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All available payment types
 */
public enum PaymentType implements BpmItem {
    CREDIT_CARD("creditcard"),
    CASH("cash"),
    CASH_ON_DELIVERY("cash_on_delivery"),
    B2B_CASH_ON_DELIVERY("b2b_cash_on_delivery"),
    AMAZON("amazonmarketplace"),
    EBAY("ebay_payment");

    private final String name;

    PaymentType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
