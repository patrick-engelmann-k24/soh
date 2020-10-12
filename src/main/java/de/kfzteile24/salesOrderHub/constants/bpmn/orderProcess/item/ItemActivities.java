package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

public enum ItemActivities implements BpmItem {
    // activities
    ;

    private final String name;

    ItemActivities(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
