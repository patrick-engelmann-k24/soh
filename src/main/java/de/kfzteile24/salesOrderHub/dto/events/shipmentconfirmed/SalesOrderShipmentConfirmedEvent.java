package de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("tracking_links")
    Collection<TrackingLink> trackingLinks;
}
