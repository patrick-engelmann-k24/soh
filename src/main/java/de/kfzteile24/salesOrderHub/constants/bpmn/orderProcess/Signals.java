package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum Signals implements BpmItem {
    CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS("signalContinueProcessingDropShipmentOrders");

    private final String name;

    Signals(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
