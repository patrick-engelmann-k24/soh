package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

@Data
public class Shipping {
    private String standard;
    private String express;
    private String bulkyGoods;
    private String dangerousGoods;
}
