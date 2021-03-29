package de.kfzteile24.salesOrderHub.constants.bpmn;

public enum ProcessDefinition {
    SALES_ORDER_PROCESS("SalesOrderProcess_v2"),
    SALES_ORDER_ROW_FULFILLMENT_PROCESS("OrderRowFulfillmentProcess");

    private final String name;

    ProcessDefinition(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
