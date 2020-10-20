package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Gateways from the Order Item Fulfillment process
 */
public enum ItemGateways implements BpmItem {
    XOR_CANCELLATION_POSSIBLE("gwXORCancellationPossible"),
    XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE("gwXORDeliveryAdressChangePossible"),
    XOR_DROP_SHIPMENT("gwXORDropShipment"),
    XOR_SHIPMENT_METHOD("gwXORShipmentMethod"),
    XOR_TOUR_STARTED("gwXORTourStarted"),
    XOR_TRACKING_ID_RECEIVED("gwXORTrackingIdReceived"),
    ;

    private final String name;

    ItemGateways(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
