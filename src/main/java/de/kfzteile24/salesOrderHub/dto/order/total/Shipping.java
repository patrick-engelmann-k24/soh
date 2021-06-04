package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Shipping {
    private BigDecimal standard;
    private BigDecimal express;
    private BigDecimal bulkyGoods;
    private BigDecimal dangerousGoods;
    private BigDecimal total;
}
