package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum ItemVariables implements BpmItem {
    // process variables
    DELIVERY_ADDRESS_CHANGE_POSSIBLE("deliveryAddressChangePossible"),
    // new delivery address, if change is possible
    DELIVERY_ADDRESS_CHANGE_REQUEST("deliveryAddressChangeRequest"),
    ITEM_CANCELLED("itemCancelled"),
    ITEM_CANCELLATION_POSSIBLE("itemCancellationPossible"),
    ITEM_DELIVERED("itemDelivered"),
    ITEM_PICKED_UP("itemDelivered"),
    ORDER_ITEM_ID("orderItemId"),
    TRACKING_ID_RECEIVED("trackingIdReceived"),
    ;

    private final String name;

    ItemVariables(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
