package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.Data;

@Data
public class FulfillmentMessage {
    String orderNumber;
    String orderItemSku;
}
