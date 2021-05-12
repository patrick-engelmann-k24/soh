package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class Discount {
    private String discountName;
    private String displayName;
    private String discountCode;
    private String discountType;
    private String promotionIdentifier;
}
