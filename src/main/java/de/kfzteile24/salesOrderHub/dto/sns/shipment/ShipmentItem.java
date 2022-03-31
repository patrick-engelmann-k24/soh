package de.kfzteile24.salesOrderHub.dto.sns.shipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class ShipmentItem {

    @JsonProperty("ParcelNumber")
    String parcelNumber;

    @JsonProperty("TrackingLink")
    String trackingLink;

    @JsonProperty("ProductNumber")
    String productNumber;

    @JsonProperty("Quantity")
    Integer quantity;
}
