package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum ItemActivities implements BpmItem {
    ACTIVITY_CHECK_CANCELLATION_POSSIBLE("activityCheckCancellationPossible"),
    ACTIVITY_HANDLE_CANCELLATION_DROPSHIPMENT("activityHandleCancellationDropshipment"),
    ACTIVITY_HANDLE_CANCELLATION_SHIPMENT("activityHandleCancellationShipment"),
    ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE("activityCheckDeliveryAddressChangePossible"),
    ACTIVITY_CHANGE_DELIVERY_ADDRESS("activityChangeDeliveryAddress"),
    ;

    private final String name;

    ItemActivities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
