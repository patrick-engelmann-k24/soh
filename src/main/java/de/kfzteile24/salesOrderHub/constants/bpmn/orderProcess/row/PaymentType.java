package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All available payment types
 */
public enum PaymentType implements BpmItem {
    CREDIT_CARD("creditCard");

    private final String name;

    PaymentType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
