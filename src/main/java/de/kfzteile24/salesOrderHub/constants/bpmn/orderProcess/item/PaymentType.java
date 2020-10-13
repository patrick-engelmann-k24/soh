package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All available payment types
 */
public enum PaymentType implements BpmItem {
    PAYMENT_CREDIT_CARD("creditCard");

    private final String name;

    PaymentType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
