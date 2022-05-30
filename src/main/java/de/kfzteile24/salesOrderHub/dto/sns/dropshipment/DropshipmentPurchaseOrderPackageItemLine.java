package de.kfzteile24.salesOrderHub.dto.sns.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderPackageItemLine {

    @JsonProperty("ProductNumber")
    private String productNumber;

    @JsonProperty("Quantity")
    private Integer quantity;
}
