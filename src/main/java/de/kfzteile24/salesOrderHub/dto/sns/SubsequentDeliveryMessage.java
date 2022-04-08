package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.deliverynote.CoreDeliveryNoteItem;
import lombok.Data;

import java.util.List;

@Data
public class SubsequentDeliveryMessage {

    @JsonProperty("OrderNumber")
    private String orderNumber;
    @JsonProperty("SubsequentDeliveryNoteNumber")
    private String subsequentDeliveryNoteNumber;
    @JsonProperty("CancellationDeliveryNoteNumber")
    private String cancellationDeliveryNoteNumber;
    @JsonProperty("Items")
    private List<CoreDeliveryNoteItem> items;
}
