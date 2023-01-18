package de.kfzteile24.salesOrderHub.dto.sns.dropshipment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DropshipmentPurchaseOrderBookedItem {

    @NotNull
    @JsonProperty("ProductNumber")
    private String productNumber;

    @NotNull
    @JsonProperty("SupplierProductNumber")
    private String supplierProductNumber;

    @JsonProperty("Quantity")
    private Integer quantity;

    @JsonProperty("PurchasePrice")
    private BigDecimal purchasePrice;
}
