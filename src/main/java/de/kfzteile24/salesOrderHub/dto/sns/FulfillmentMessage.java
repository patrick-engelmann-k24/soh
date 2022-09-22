package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.Data;

@Data
public class FulfillmentMessage {
    private String orderNumber;
    private String orderItemSku;
}
