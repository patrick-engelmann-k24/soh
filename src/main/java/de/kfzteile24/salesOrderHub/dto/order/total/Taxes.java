package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

@Data
public class Taxes {
    String type;
    String value;
    String rate;
}
