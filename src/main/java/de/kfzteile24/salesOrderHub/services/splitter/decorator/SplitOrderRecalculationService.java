package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.splitter.SplitterException;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SplitOrderRecalculationService extends AbstractSplitDecorator {

    private SalesOrderService salesOrderService;

    private OrderUtil orderUtil;

    @Override
    public void processOrderList(ArrayList<Order> orderList) {
        if (onlyRegularItemsInOriginalOrder(orderList) || onlyDropShipmentItemsInOriginalOrder(orderList)) {
            // no recalculation totals is needed
        } else if (orderList.size() == 2) {
            Order originalOrder = orderList.get(0);
            Order splittedOrder = orderList.get(1);
            salesOrderService.recalculateTotals(originalOrder);
            salesOrderService.recalculateTotalsForSplittedOrder(splittedOrder);
        } else {
            throw new SplitterException(new IllegalArgumentException("The order, which contains both regular and dropshipment items " +
                    "must be splitted in exactly two orders. Please, review the code, the implemenation has been changed by someone and" +
                    "this code doesn't work correctly anymore!!!"));
        }
    }

    public void recalculateOrder(Order order) {
        salesOrderService.recalculateTotals(order);
    }

    private boolean onlyRegularItemsInOriginalOrder(ArrayList<Order> orderList) {
        return orderList.size() == 1 && orderUtil.containsOnlyRegularItems(orderList.get(0));
    }

    private boolean onlyDropShipmentItemsInOriginalOrder(ArrayList<Order> orderList) {
        return orderList.size() == 1 && orderUtil.containsOnlyDropShipmentItems(orderList.get(0));
    }

}
