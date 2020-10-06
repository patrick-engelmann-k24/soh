package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum ItemVariables implements BpmItem {
    // process variables
    VAR_ITEM_CANCELLED("itemCancelled"),
    VAR_ITEM_ID("orderItemId"),
    VAR_ITEM_CANCELLATION_POSSIBLE("itemCancellationPossible");


    private final String name;

    ItemVariables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
