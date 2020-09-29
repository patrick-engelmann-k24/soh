package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class Discount {
    String discountName;
    String displayName;
    String discountCode;
    String discountType;
    String promotionIdentifier;
}
