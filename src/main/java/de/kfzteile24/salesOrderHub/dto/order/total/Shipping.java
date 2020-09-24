package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

@Data
public class Shipping {
    String standard;
    String express;
    String bulkyGoods;
    String dangerousGoods;
}
