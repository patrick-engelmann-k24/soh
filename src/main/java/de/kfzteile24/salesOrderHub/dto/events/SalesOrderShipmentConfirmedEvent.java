package de.kfzteile24.salesOrderHub.dto.events;

import de.kfzteile24.soh.order.dto.Order;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Builder
@Value
@Jacksonized
public class SalesOrderShipmentConfirmedEvent {

    Order order;

    @Singular
    Collection<String> trackingLinks;
}
