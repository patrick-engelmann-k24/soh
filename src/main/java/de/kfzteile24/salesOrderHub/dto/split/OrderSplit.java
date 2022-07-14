package de.kfzteile24.salesOrderHub.dto.split;

import de.kfzteile24.soh.order.dto.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderSplit {

    private Order order;

    private boolean isSplitted;

    public static OrderSplit splittedOrder(Order order) {
        return new OrderSplit(order, true);
    }

    public static OrderSplit regularOrder(Order order) {
        return new OrderSplit(order, false);
    }
}
