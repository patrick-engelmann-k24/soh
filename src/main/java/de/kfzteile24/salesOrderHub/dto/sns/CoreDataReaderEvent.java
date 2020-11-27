package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CoreDataReaderEvent {
    String orderNumber;
    String deliveryNoteNumber;
    String invoiceNumber;
    String orderItemSku;
    LocalDateTime createdAt;
}
