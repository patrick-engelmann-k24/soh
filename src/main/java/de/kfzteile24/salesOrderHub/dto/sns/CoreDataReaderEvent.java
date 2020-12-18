package de.kfzteile24.salesOrderHub.dto.sns;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CoreDataReaderEvent {
    String orderNumber;
    String deliveryNoteNumber;
    String invoiceNumber;
    String orderItemSku;
    LocalDateTime createdAt;
}
