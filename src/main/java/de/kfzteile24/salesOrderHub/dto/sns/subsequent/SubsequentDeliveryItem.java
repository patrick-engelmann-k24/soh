package de.kfzteile24.salesOrderHub.dto.sns.subsequent;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubsequentDeliveryItem {
    private String sku;
    private BigDecimal quantity;
}
