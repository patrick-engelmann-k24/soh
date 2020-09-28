package de.kfzteile24.salesOrderHub.dto.order.header;

import lombok.Data;

@Data
public class Creator {
    String type;
    String creatorId;
    String creatorName;
}
