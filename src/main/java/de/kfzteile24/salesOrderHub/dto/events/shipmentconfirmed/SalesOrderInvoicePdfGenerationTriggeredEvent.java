package de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed;

import de.kfzteile24.soh.order.dto.Order;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Builder
@Value
@Jacksonized
public class SalesOrderInvoicePdfGenerationTriggeredEvent {

    Order order;
}

