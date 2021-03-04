package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All Subprocesses from the Order Item Fullfilment process
 */
public enum BPMSalesOrderRowFulfillment implements BpmItem {
    SUB_PROCESS_HANDLE_ORDER_ROW_CANCELLATION("subProcessHandleOrderRowCancellation"),
    SUB_PROCESS_ORDER_ROW_CANCELLATION_SHIPMENT("subProcessOrderRowCancellationShipment"),
    SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE("subProcessOrderRowDeliveryAddressChange");

    private final String name;

    BPMSalesOrderRowFulfillment(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
