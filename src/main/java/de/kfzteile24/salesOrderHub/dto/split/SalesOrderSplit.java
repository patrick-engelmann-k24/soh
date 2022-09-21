package de.kfzteile24.salesOrderHub.dto.split;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SalesOrderSplit {

    private final SalesOrder order;

    private final boolean isSplitted;

    public static SalesOrderSplit regularOrder(SalesOrder order) {
        return new SalesOrderSplit(order, false);
    }
}
