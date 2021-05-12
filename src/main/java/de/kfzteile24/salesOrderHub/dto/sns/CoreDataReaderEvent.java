package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.Data;

@Data
public class CoreDataReaderEvent {
    private String orderNumber;
    private String deliveryNoteNumber;
    private String invoiceNumber;
    private String orderItemSku;
    private String createdAt;
}
