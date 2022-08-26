package de.kfzteile24.salesOrderHub.dto.sns.shared;

import java.math.BigDecimal;

public interface OrderItem {

    String getItemNumber();

    BigDecimal getQuantity();

    BigDecimal getUnitNetAmount();

    BigDecimal getLineNetAmount();

    BigDecimal getUnitGrossAmount();

    BigDecimal getLineGrossAmount();

    BigDecimal getTaxRate();

    Boolean getIsShippingCost();

    String getDescription();
}
