package de.kfzteile24.salesOrderHub.dto.sns.subsequent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.Digits;
import java.math.BigDecimal;

@Data
public class SubsequentDeliveryItem {

    @JsonProperty("Sku")
    private String sku;

    @JsonProperty("Quantity")
    private BigDecimal quantity;

    @JsonProperty("UnitPriceGross")
    @Digits(integer = 9, fraction = 2)
    private BigDecimal unitPriceGross;

    @JsonProperty("SalesPriceGross")
    @Digits(integer = 9, fraction = 2)
    private BigDecimal salesPriceGross;

    @JsonProperty("TaxRate")
    @Digits(integer = 9, fraction = 2)
    private BigDecimal taxRate;

}
