package de.kfzteile24.salesOrderHub.dto.events;

import de.kfzteile24.soh.order.dto.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Jacksonized
public class OrderCancelledEvent {

    private Order order;
}
