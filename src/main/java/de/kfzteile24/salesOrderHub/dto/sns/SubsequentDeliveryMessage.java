package de.kfzteile24.salesOrderHub.dto.sns;

import de.kfzteile24.salesOrderHub.dto.sns.subsequent.SubsequentDeliveryItem;
import lombok.Data;

import java.util.List;

@Data
public class SubsequentDeliveryMessage {
    private String orderNumber;
    private String subsequentDeliveryNoteNumber;
    private String cancellationDeliveryNoteNumber;
    private List<SubsequentDeliveryItem> items;
}
