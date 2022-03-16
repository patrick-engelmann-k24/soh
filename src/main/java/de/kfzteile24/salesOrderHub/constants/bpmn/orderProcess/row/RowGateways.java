package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Gateways from the Order Item Fulfillment process
 */
public enum RowGateways implements BpmItem {
    XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE("gwXORDeliveryAdressChangePossible"),
    XOR_DROP_SHIPMENT("gwXORDropShipment"),
    XOR_SHIPMENT_METHOD("gwXORShipmentMethod"),
    XOR_TOUR_STARTED("gwXORTourStarted"),
    XOR_TRACKING_ID_RECEIVED("gwXORTrackingIdReceived"),
    XOR_CLICK_AND_COLLECT("gwXORClickAndCollect")
    ;

    private final String name;

    RowGateways(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
