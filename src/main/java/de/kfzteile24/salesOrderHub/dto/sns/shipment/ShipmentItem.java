package de.kfzteile24.salesOrderHub.dto.sns.shipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentItem {

    @JsonProperty("ParcelNumber")
    private String parcelNumber;

    @JsonProperty("TrackingLink")
    private String trackingLink;

    @JsonProperty("ProductNumber")
    private String productNumber;

    @JsonProperty("Quantity")
    private Integer quantity;

    @JsonProperty("ServiceProviderName")
    private String serviceProviderName;
}
