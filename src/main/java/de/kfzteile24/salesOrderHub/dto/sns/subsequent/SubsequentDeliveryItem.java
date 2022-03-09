package de.kfzteile24.salesOrderHub.dto.sns.subsequent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubsequentDeliveryItem {
    @JsonProperty("Sku")
    private String sku;
    @JsonProperty("Quantity")
    private BigDecimal quantity;
}
