package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class Creator {
    private String type;
    private String creatorId;
    private String creatorName;
}
