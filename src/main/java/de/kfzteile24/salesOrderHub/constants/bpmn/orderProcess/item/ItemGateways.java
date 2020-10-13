package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Gateways from the Order Item Fullfilment process
 */
public enum ItemGateways implements BpmItem {
    // events

    // Gateways,
    GW_XOR_SHIPMENT_METHOD("gwXORShipmentMethod"),
    GW_XOR_CANCELLATION_POSSIBLE("gwXORCancellationPossible"),
    GW_XOR_DROP_SHIPMENT("gwXORDropShipment"),
    GW_XOR_TRACKING_ID_RECEIVED("gwXORTrackingIdReceived"),
    GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE("gwXORDeliveryAdressChangePossible"),
    GW_XOR_TOUR_STARTED("gwXORTourStarted"),
    ;

    private final String name;

    ItemGateways(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
