package de.kfzteile24.salesOrderHub.dto.sns.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderPackageItemLine {

    @NotNull
    @JsonProperty("ProductNumber")
    private String productNumber;

    @NotNull
    @JsonProperty("Quantity")
    private Integer quantity;
}
