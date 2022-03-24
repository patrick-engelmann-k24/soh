package de.kfzteile24.salesOrderHub.dto.sns;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.kfzteile24.salesOrderHub.dto.sns.cancellation.CoreCancellationItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoreCancellationMessage {

    @JsonProperty("OrderNumber")
    private String orderNumber;

    @JsonProperty("CancellationDeliveryNoteNumber")
    private String cancellationDeliveryNoteNumber;

    @JsonProperty("OriginalDeliveryNoteNumber")
    private String originalDeliveryNoteNumber;

    @JsonProperty("Items")
    private List<CoreCancellationItem> items;
}
