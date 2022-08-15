package de.kfzteile24.salesOrderHub.dto.sns.deliverynote;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoreDeliveryNoteItem {

    @JsonProperty("Sku")
    private String sku;

    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @JsonProperty("UnitPriceGross")
    private BigDecimal unitPriceGross;

    @JsonProperty("SalesPriceGross")
    private BigDecimal salesPriceGross;

    @JsonProperty("TaxRate")
    private BigDecimal taxRate;
}
