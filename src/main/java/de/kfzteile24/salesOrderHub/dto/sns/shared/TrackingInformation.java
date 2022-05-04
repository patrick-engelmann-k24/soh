package de.kfzteile24.salesOrderHub.dto.sns.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class TrackingInformation {

    @JsonProperty("ShippingProvider")
    String shippingProvider;

    @JsonProperty("TrackingNumber")
    String trackingNumber;
}
