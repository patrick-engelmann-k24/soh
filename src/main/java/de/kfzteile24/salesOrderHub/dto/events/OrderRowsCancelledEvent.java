package de.kfzteile24.salesOrderHub.dto.events;

import de.kfzteile24.soh.order.dto.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Jacksonized
public class OrderRowsCancelledEvent {
    private Boolean isFullCancellation;
    private List<String> cancelledRows;
    private Order order;
    private OffsetDateTime cancellationDate;
}
