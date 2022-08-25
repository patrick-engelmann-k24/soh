package de.kfzteile24.salesOrderHub.dto.sns.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrackingInformation {

    @JsonProperty("ShippingProvider")
    private String shippingProvider;

    @JsonProperty("TrackingNumber")
    private String trackingNumber;
}
