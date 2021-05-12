package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class Origin {
    private String salesChannel;
    private String locale;
}
