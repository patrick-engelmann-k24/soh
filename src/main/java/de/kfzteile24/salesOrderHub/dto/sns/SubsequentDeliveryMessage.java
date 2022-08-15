package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.deliverynote.CoreDeliveryNoteItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
