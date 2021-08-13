package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Discount {
    private String discountName;
    private String displayName;
    private String discountCode;
    private String discountType;
    private String promotionIdentifier;
    private BigDecimal discountValueGross;
}
