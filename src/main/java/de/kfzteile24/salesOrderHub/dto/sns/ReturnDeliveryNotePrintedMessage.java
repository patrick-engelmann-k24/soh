package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.deliverynote.CoreDeliveryNoteItem;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Collection;

@Builder
@Value
@Jacksonized
public class ReturnDeliveryNotePrintedMessage {

    @JsonProperty("OrderNumber")
    String orderNumber;

    @JsonProperty("ReturnDeliveryNoteNumber")
    String returnDeliveryNoteNumber;

    @JsonProperty("Items")
    Collection<CoreDeliveryNoteItem> items;
}
