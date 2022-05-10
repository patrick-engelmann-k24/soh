package de.kfzteile24.salesOrderHub.dto.sns.shared;

import java.math.BigDecimal;

public interface OrderItem {

    public String getItemNumber();

    public BigDecimal getQuantity();

    public BigDecimal getUnitNetAmount();

    public BigDecimal getTaxRate();





}
