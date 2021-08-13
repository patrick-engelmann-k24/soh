package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ShippingTotalValues {
    private BigDecimal standard;
    private BigDecimal express;
    private BigDecimal total;
    private BigDecimal bulkyGoods;
}
