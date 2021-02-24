package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum ItemSignals implements BpmItem {
    SIGNAL_RECEIVE_ORDER_ITEM_CANCELLATION("signalReceiveOrderItemCancellation");

    private final String name;

    ItemSignals(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
