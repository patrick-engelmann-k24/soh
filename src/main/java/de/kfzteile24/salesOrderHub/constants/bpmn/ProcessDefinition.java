package de.kfzteile24.salesOrderHub.constants.bpmn;

public enum ProcessDefinition {
    SALES_ORDER_PROCESS("SalesOrderProcess_v2"),
    SALES_ORDER_ITEM_FULFILLMENT_PROCESS("OrderItemFulfillmentProcess")
    ;

    private final String name;

    ProcessDefinition(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
