package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This class splits single orders into multiple orders (
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderSplitService extends AbstractSplitDecorator {
    public static final String ORDER_FULFILLMENT_DELTICOM = "delticom";

    private final ObjectMapperConfig objectMapperConfig;

    private final OrderUtil orderUtil;

    @Override
    public void processOrderList(ArrayList<Order> orderList) {
        List<Order> splittedOrders = new ArrayList<>();
        for (Order order: orderList) {
           Order dsOrder = splitOrderIfNecessary(order);
           if (dsOrder != null){
               splittedOrders.add((dsOrder));
           }
        }
        for (Order order: splittedOrders) {
            orderList.add(order);
        }
    }

    public Order splitOrderIfNecessary(final Order currentOrder) {

        // Scenario 1: There are only dropshipment items (no split is needed, change fulfillment to delticom)
        // Scenario 2: There are some dropshipment items (split as usual)
        // Scenario 3: There are no dropshipment items (no split is needed)
        if (orderUtil.containsOnlyDropShipmentItems(currentOrder)) {
            updateExistingOrder(currentOrder);
        } else if (orderUtil.containsDropShipmentItems(currentOrder)) {
            final var dsOrder = copyOrderFromExistingOrder(currentOrder);

            List<OrderRows> toRemove = new ArrayList<>();
            for (final var orderRows : currentOrder.getOrderRows()) {
                if (orderUtil.isDropShipmentItem(orderRows, currentOrder.getOrderHeader().getPlatform())) {
                    dsOrder.getOrderRows().add(orderRows);
                    toRemove.add(orderRows);
                }
            }

            currentOrder.getOrderRows().removeAll(toRemove);

            return dsOrder;
        }

        return null;
    }

    /**
     * make a copy of an existing order
     *
     * @return the copied order
     */
    private Order copyOrderFromExistingOrder(Order originalOrder) {
        Order copy = objectMapperConfig.objectMapper().convertValue(originalOrder, Order.class);
        copy.getOrderHeader().setOrderNumber(originalOrder.getOrderHeader().getOrderNumber().concat("-1"));
        copy.getOrderHeader().setOrderId(UUID.randomUUID());
        copy.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_DELTICOM);
        copy.getOrderRows().clear();
        return copy;
    }

    private Order updateExistingOrder(Order originalOrder) {
        originalOrder.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_DELTICOM);
        return originalOrder;
    }
}
