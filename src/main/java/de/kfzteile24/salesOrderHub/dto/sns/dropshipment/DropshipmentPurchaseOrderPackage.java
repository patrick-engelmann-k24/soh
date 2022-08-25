package de.kfzteile24.salesOrderHub.dto.sns.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderPackage {

    @JsonProperty("TrackingLink")
    private String trackingLink;

    @JsonProperty("Items")
    private List<DropshipmentPurchaseOrderPackageItemLine> items;
}
