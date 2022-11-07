package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        var salesOrder = salesOrderRowService.cancelOrder(orderNumber);
        snsPublishService.publishOrderCancelled(salesOrder.getLatestJson());
    }
}
