package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationSalesOrderService {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    private final SalesOrderMapper salesOrderMapper;

    private final OrderUtil orderUtil;

    @Transactional
    @EnrichMessageForDlq
    public void handleMigrationCoreSalesOrderCreated(Order order, MessageWrapper messageWrapper) throws JsonProcessingException {
        Order originalOrder = orderUtil.copyOrderJson(order);
        String orderNumber = order.getOrderHeader().getOrderNumber();

        salesOrderService.getOrderByOrderNumber(orderNumber)
                .ifPresentOrElse(salesOrder -> {
                    salesOrderService.enrichSalesOrder(salesOrder, order, originalOrder);
                    salesOrderService.save(salesOrder, MIGRATION_SALES_ORDER_RECEIVED);
                    log.info("Order with order number: {} is duplicated for migration. Publishing event on " +
                            "migration topic", orderNumber);
                }, () -> {
                    var salesOrder = salesOrderMapper.map(order);
                    salesOrderService.enrichSalesOrder(salesOrder, order, originalOrder);
                    log.info("Order with order number: {} is a new migration order. No process will be created.",
                            orderNumber);
                    salesOrderService.createSalesOrder(salesOrder);
                });

        snsPublishService.publishMigrationOrderCreated(orderNumber);
    }
}