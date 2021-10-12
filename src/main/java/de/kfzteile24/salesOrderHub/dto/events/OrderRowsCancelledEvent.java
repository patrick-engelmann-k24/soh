package de.kfzteile24.salesOrderHub.dto.events;

import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
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
    private List<OrderRows> cancelledRows;
    private Order order;
    private OffsetDateTime cancellationDate;
}
