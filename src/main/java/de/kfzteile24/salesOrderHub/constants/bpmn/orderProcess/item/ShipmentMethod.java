package de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;

/**
 * All available payment types
 */
public enum ShipmentMethod implements BpmItem {
    // todo: what are the "official" names of shipment methods?
    SHIPMENT_REGULAR("shipment_regular"),
    SHIPMENT_EXPRESS("shipment_express"),
    OWN_DELIVERY("own_delivery"),
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
