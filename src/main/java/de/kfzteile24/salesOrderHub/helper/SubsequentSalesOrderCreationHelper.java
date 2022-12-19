package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubsequentSalesOrderCreationHelper {

    private final OrderUtil orderUtil;

    public OrderHeader createOrderHeader(SalesOrder salesOrder, String newOrderNumber, String invoiceNumber) {
        var orderJson = orderUtil.copyOrderJson(salesOrder.getLatestJson());

        orderJson.getOrderHeader().setPlatform(Platform.SOH);
        orderJson.getOrderHeader().setOrderNumber(newOrderNumber);
        orderJson.getOrderHeader().setOrderGroupId(salesOrder.getOrderGroupId());
        orderJson.getOrderHeader().setDocumentRefNumber(invoiceNumber);

        return orderJson.getOrderHeader();
    }

    public static SalesOrder buildSubsequentSalesOrder(Order order, String newOrderNumber){
        return SalesOrder.builder()
                .orderNumber(newOrderNumber)
                .orderGroupId(order.getOrderHeader().getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(getCustomerEmailByOrderJson(order))
                .originalOrder(order)
                .latestJson(order)
                .build();
    }

    public static String getCustomerEmailByOrderJson(Order order) {
        if (order.getOrderHeader().getCustomer() != null) {
            return order.getOrderHeader().getCustomer().getCustomerEmail();
        }
        throw new NotFoundException("Customer Email is not found for the order number: " +
                order.getOrderHeader().getOrderNumber());
    }
}
