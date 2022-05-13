package de.kfzteile24.salesOrderHub.constants.bpmn;

public enum SalesOrderConsts implements BpmItem {
    TAXES_PERCENT_19("NORMAL"),
    TAXES_EXCHANGE_PART("EXCHANGE_PART");


    private final String name;

    SalesOrderConsts(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
