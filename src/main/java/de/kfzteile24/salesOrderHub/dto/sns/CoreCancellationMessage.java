package de.kfzteile24.salesOrderHub.dto.sns;

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

    private String orderNumber;

    private String cancellationDeliveryNoteNumber;

    private String originalDeliveryNoteNumber;

    private List<CoreCancellationItem> items;
}
