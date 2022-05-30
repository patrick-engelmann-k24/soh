package de.kfzteile24.salesOrderHub.dto.events;

import de.kfzteile24.salesOrderHub.dto.events.dropshipment.DropshipmentOrderPackage;
import de.kfzteile24.soh.order.dto.Order;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Value
@Jacksonized
public class DropshipmentOrderReturnNotifiedEvent {

    Order order;
    List<DropshipmentOrderPackage> packages;
}
