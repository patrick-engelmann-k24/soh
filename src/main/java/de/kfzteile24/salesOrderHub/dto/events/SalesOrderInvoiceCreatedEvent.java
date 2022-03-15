package de.kfzteile24.salesOrderHub.dto.events;

import de.kfzteile24.soh.order.dto.Order;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class SalesOrderInvoiceCreatedEvent {

    Order order;
}
