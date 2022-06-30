package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.ItemSplitService;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.SplitOrderRecalculationService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService.ORDER_FULFILLMENT_K24;

@Service
@Slf4j
@RequiredArgsConstructor
public class SplitterService {

    private final ItemSplitService itemSplitService;

    private final OrderSplitService orderSplitService;

    private final SplitOrderRecalculationService splitOrderRecalculationService;

    private final FeatureFlagConfig featureFlagConfig;

    /**
     * Split up the order and set items. Recalculates also the partial orders and starts the business process(es)
     * 1. step -> split sets into line items
     * 2. step -> split drop shipment items into separate orders     *
     *
     * @param originOrder the initial order
     * @return list of split SalesOrders
     */
    public List<SalesOrder> splitSalesOrder(final Order originOrder) {
        updateOriginalOrder(originOrder);
        final var orderList = new ArrayList<Order>();
        orderList.add(originOrder);
        if (featureFlagConfig.getIgnoreSalesOrderSplitter()) {
            log.info("Sales Order Splitter is ignored");
        } else {
            // add further splitters here (all operations happening on the list object)
            if (!featureFlagConfig.getIgnoreSetDissolvement())
            {
                itemSplitService.processOrderList(orderList);
            }

            Order order = orderList.stream().findFirst().orElseThrow();
            if (order.getOrderHeader().getPlatform() == Platform.BRAINCRAFT) {
                orderSplitService.processOrderList(orderList);
            }

            if (orderList.size() > 1) {
                //recalculate totals only if we added splitted order, which means we also changed original order
                splitOrderRecalculationService.processOrderList(orderList);
            }
        }

        return convert2SalesOrderList(originOrder, orderList);
    }

    /**
     * Splits the order into several other (drop shipment-)orders
     *
     * @param originOrder the initial order
     * @return list of split SalesOrders
     */
    private List<SalesOrder> convert2SalesOrderList(final Order originOrder, final ArrayList<Order> splittedOrderList) {
        List<SalesOrder> list = new ArrayList<>();
        for(final var splitOrder: splittedOrderList) {
            list.add(buildSalesOrder(splitOrder, originOrder));
        }
        return list;
    }

    private SalesOrder buildSalesOrder(Order splitOrder, Order originOrder) {
        return SalesOrder
                .builder()
                .orderNumber(splitOrder.getOrderHeader().getOrderNumber())
                .orderGroupId(originOrder.getOrderHeader().getOrderGroupId())
                .salesChannel(splitOrder.getOrderHeader().getSalesChannel())
                .customerEmail(splitOrder.getOrderHeader().getCustomer().getCustomerEmail())
                .originalOrder(originOrder)
                .latestJson(/*splitted order goes here */ splitOrder)
                .build();
    }

    private Order updateOriginalOrder(Order originalOrder) {
        originalOrder.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        return originalOrder;
    }
}
