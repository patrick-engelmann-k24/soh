package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.Data;

@Data
public class CoreDataReaderEvent {
    String orderNumber;
    String deliveryNoteNumber;
    String invoiceNumber;
    String orderItemSku;
    String createdAt;

    public CoreDataReaderEvent() {
    }
}
