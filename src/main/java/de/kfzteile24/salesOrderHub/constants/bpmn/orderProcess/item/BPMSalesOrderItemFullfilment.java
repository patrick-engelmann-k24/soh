package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Subprocesses from the Order Item Fullfilment process
 */
public enum BPMSalesOrderItemFullfilment implements BpmItem {
    SUB_PROCESS_HANDLE_ORDER_ITEM_CANCELLATION("subProcessHandleOrderItemCancellation"),
    SUB_PROCESS_ORDER_ITEM_CANCELLATION_SHIPMENT("subProcessOrderItemCancellationShipment"),
    SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE("subProcessOrderItemDeliveryAddressChange");

    private final String name;

    BPMSalesOrderItemFullfilment(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
