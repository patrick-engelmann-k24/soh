package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCreationHelper{

    private final OrderUtil orderUtil;
    private final SalesOrderService salesOrderService;

    public Order createOrderHeader(SalesOrder salesOrder, String newOrderNumber, String invoiceNumber) {
        var orderJson = orderUtil.copyOrderJson(salesOrder.getLatestJson());

        orderJson.getOrderHeader().setPlatform(Platform.SOH);
        orderJson.getOrderHeader().setOrderNumber(newOrderNumber);
        // order.getOrderHeader().setOrderGroupId(originalSalesOrder.getOrderGroupId()); put separately in createSalesOrderForInvoice
        orderJson.getOrderHeader().setDocumentRefNumber(invoiceNumber);

        return orderJson;
    }

    public SalesOrder buildSubsequentOrder(SalesOrder salesOrder, String newOrderNumber, String invoiceNumber){

        var orderJson = createOrderHeader(salesOrder, newOrderNumber, invoiceNumber);

        var customerEmail = Strings.isNotEmpty(salesOrder.getCustomerEmail()) ?
                salesOrder.getCustomerEmail() :
                salesOrderService.getCustomerEmailByOrderJson(orderJson);
        var subsequentOrder = SalesOrder.builder()
                .orderNumber(newOrderNumber)
                .orderGroupId(salesOrder.getOrderGroupId())
                .salesChannel(salesOrder.getSalesChannel())
                .customerEmail(customerEmail)
                .originalOrder(orderJson)
                .latestJson(orderJson)
               // .processId(activityInstanceId)
                .build();
        return subsequentOrder;
    }
}
