package de.kfzteile24.salesOrderHub.domain.pdh.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSet {

    @JsonProperty("k24_sku")
    private String sku;

    @JsonProperty("amount")
    private BigDecimal quantity;
}
