package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum ItemVariables implements BpmItem {
    // process variables
    VAR_ITEM_CANCELLED("itemCancelled"),
    ORDER_ITEM_ID("orderItemId"),
    VAR_ITEM_CANCELLATION_POSSIBLE("itemCancellationPossible"),
    TRACKING_ID_RECEIVED("trackingIdReceived"),
    DELIVERY_ADDRESS_CHANGE_POSSIBLE("deliveryAddressChangePossible"),
    ITEM_DELIVERED("itemDelivered"),
    ITEM_PICKED_UP("itemDelivered");

    private final String name;

    ItemVariables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
