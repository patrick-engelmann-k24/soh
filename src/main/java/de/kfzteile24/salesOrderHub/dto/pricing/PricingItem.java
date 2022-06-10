package de.kfzteile24.salesOrderHub.dto.pricing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class PricingItem {

    @JsonProperty("unit_prices")
    private Prices unitPrices;

    @JsonProperty("value_share")
    private BigDecimal valueShare;

    @JsonProperty("product_number")
    private String sku;
}
