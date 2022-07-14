package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.dto.split.OrderSplit;
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
public class OrderSplitService {
    public static final String ORDER_FULFILLMENT_DELTICOM = "delticom";

    public static final String ORDER_FULFILLMENT_K24 = "K24";

    private final ObjectMapperConfig objectMapperConfig;

    private final OrderUtil orderUtil;

    public void processOrderList(List<OrderSplit> orderSplitList) {
        List<OrderSplit> splittedOrders = new ArrayList<>();
        for (OrderSplit orderSplit: orderSplitList) {
           Order dsOrder = splitOrderIfNecessary(orderSplit.getOrder());
           if (dsOrder != null){
               splittedOrders.add((OrderSplit.splittedOrder(dsOrder)));
           }
        }
        for (OrderSplit orderSplit: splittedOrders) {
            orderSplitList.add(orderSplit);
        }
    }

    public Order splitOrderIfNecessary(final Order currentOrder) {

        // Scenario 1: There are only dropshipment items (no split is needed, change fulfillment to delticom)
        // Scenario 2: There are some dropshipment items (split as usual)
        // Scenario 3: There are no dropshipment items (no split is needed)
        if (orderUtil.containsOnlyDropShipmentItems(currentOrder)) {
            updateDropshipmentOrder(currentOrder);
        } else if (orderUtil.containsDropShipmentItems(currentOrder)) {
            final var dsOrder = copyDropshipmentOrderFromExistingOrder(currentOrder);

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
    private Order copyDropshipmentOrderFromExistingOrder(Order originalOrder) {
        Order copy = objectMapperConfig.objectMapper().convertValue(originalOrder, Order.class);
        updateDropshipmentOrder(copy);
        copy.getOrderHeader().setOrderNumber(originalOrder.getOrderHeader().getOrderNumber().concat("-1"));
        copy.getOrderHeader().setOrderId(UUID.randomUUID());
        copy.getOrderRows().clear();
        return copy;
    }

    private Order updateDropshipmentOrder(Order originalOrder) {
        originalOrder.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_DELTICOM);
        return originalOrder;
    }
}
