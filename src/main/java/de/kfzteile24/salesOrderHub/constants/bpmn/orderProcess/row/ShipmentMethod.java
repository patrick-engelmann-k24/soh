package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All available payment types
 */
public enum ShipmentMethod implements BpmItem {
    REGULAR("shipment_regular"),
    EXPRESS("shipment_express"),
    DIRECT_DELIVERY("direct_delivery"),
    CLICK_COLLECT("click_collect"),
    // sometimes we have not shipment method. But we can't return null in fromString
    UNKNOWN("unknown");

    private final String name;

    ShipmentMethod(final String name) {
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public static ShipmentMethod fromString(String text) {
        for (ShipmentMethod method : ShipmentMethod.values()) {
            if (method.name.equalsIgnoreCase(text)) {
                return method;
            }
        }
        return UNKNOWN;
    }

}
