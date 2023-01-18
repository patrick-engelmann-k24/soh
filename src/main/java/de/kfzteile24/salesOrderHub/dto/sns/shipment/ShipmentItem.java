package de.kfzteile24.salesOrderHub.dto.sns.shipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentItem {

    @NotNull
    @JsonProperty("ParcelNumber")
    private String parcelNumber;

    @NotNull
    @JsonProperty("TrackingLink")
    private String trackingLink;

    @NotNull
    @JsonProperty("ProductNumber")
    private String productNumber;

    @NotNull
    @JsonProperty("Quantity")
    private Integer quantity;

    @NotNull
    @JsonProperty("ServiceProviderName")
    private String serviceProviderName;
}
