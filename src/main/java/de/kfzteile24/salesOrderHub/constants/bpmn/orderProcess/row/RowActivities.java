package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum RowActivities implements BpmItem {
    CHANGE_DELIVERY_ADDRESS("activityChangeDeliveryAddress"),
    CHECK_CANCELLATION_POSSIBLE("activityCheckCancellationPossible"),
    CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE("activityCheckDeliveryAddressChangePossible"),
    HANDLE_CANCELLATION_SHIPMENT("activityHandleCancellationShipment"),
    ;

    private final String name;

    RowActivities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
