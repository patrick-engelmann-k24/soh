package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class SplitOrderRecalculationService extends AbstractSplitDecorator {

    private final SalesOrderService salesOrderService;

    private final OrderUtil orderUtil;

    @Override
    public void processOrderList(ArrayList<Order> orderList) {
        for (Order order: orderList) {
            if (orderUtil.containsDropShipmentItems(order)) {
                //splitted order
                salesOrderService.recalculateTotals(order, BigDecimal.ZERO, BigDecimal.ZERO, true);
            } else {
                //original order
                BigDecimal shippingCostNet = order.getOrderHeader().getTotals().getShippingCostNet();
                BigDecimal shippingCostGross = order.getOrderHeader().getTotals().getShippingCostGross();
                salesOrderService.recalculateTotals(order, shippingCostNet, shippingCostGross, true);
            }
        }
    }

}
