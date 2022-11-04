package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.toList;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderCancelledService {

    private final SalesOrderService salesOrderService;
    private final SnsPublishService snsPublishService;

    private final SalesOrderRowService salesOrderRowService;

    @EnrichMessageForDlq
    public void handleCoreSalesOrderCancelled(CoreSalesOrderCancelledMessage message, MessageWrapper messageWrapper) {
        final var orderNumber = message.getOrderNumber();
        log.info("Received core sales order cancelled message with order number: {}", orderNumber);
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        salesOrder = cancelOrder(salesOrder);
        snsPublishService.publishOrderCancelled(salesOrder.getLatestJson());
    }

    private SalesOrder cancelOrder(SalesOrder salesOrder) {
        var orderNumber = salesOrder.getOrderNumber();
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        salesOrderRowService.cancelOrder(salesOrder);
        salesOrderRowService.cancelOrderRows(orderNumber, orderRowSkus);
        return salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
    }
}
