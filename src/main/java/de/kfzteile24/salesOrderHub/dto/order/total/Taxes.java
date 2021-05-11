package de.kfzteile24.salesOrderHub.dto.order.total;

import lombok.Data;

@Data
public class Taxes {
    private String type;
    private String value;
    private String rate;
}
