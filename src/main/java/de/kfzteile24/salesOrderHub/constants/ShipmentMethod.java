package de.kfzteile24.salesOrderHub.constants;

import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipmentMethod implements BpmItem {
    REGULAR("shipment_regular"),
    EXPRESS("shipment_express"),
    PRIORITY("shipment_priority"),
    DIRECT_DELIVERY("direct_delivery"),
    CLICK_COLLECT("click_and_collect"),
    NONE("none"),
    UNKNOWN("unknown");

    private final String name;

}
