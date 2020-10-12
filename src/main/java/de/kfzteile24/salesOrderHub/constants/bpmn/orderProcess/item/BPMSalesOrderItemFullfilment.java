package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * todo: remove class
 * All Activities, Gateways, Events from the Order Item Fullfilment process
 */
public enum BPMSalesOrderItemFullfilment implements BpmItem {
    // events

    // Gateways,
    GW_XOR_SHIPMENT_METHOD("gwXORShipmentMethod"),
    GW_XOR_CANCELLATION_POSSIBLE("gwXORCancellationPossible"),
    GW_XOR_DROP_SHIPMENT("gwXORDropShipment"),
    GW_XOR_TRACKING_ID_RECEIVED("gwXORTrackingIdReceived"),
    GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE("gwXORDeliveryAdressChangePossible"),
    GW_XOR_TOUR_STARTED("gwXORTourStarted"),

    SUB_PROCESS_ORDER_ITEM_CANCELLATION_DROPSHIPMENT("subProcessOrderItemCancellationDropshipment"),
    SUB_PROCESS_HANDLE_ORDER_ITEM_CANCELLATION("subProcessHandleOrderItemCancellation"),
    SUB_PROCESS_ORDER_ITEM_CANCELLATION_SHIPMENT("subProcessOrderItemCancellationShipment"),
    // todo: name correctly in BPMN
    SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRES_CHANGE("Activity_1r18tfj")
    ;

    private final String name;

    BPMSalesOrderItemFullfilment(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
