package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
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
    public void handleMigrationCoreSalesOrderCreated(Order message) throws JsonProcessingException {
        Order originalOrder = orderUtil.copyOrderJson(message);
        String orderNumber = message.getOrderHeader().getOrderNumber();

        salesOrderService.getOrderByOrderNumber(orderNumber)
                .ifPresentOrElse(salesOrder -> {
                    salesOrderService.enrichSalesOrder(salesOrder, message, originalOrder);
                    salesOrderService.save(salesOrder, MIGRATION_SALES_ORDER_RECEIVED);
                    log.info("Order with order number: {} is duplicated for migration. Publishing event on " +
                            "migration topic", orderNumber);
                }, () -> {
                    var salesOrder = salesOrderMapper.map(message);
                    salesOrderService.enrichSalesOrder(salesOrder, message, originalOrder);
                    log.info("Order with order number: {} is a new migration order. No process will be created.",
                            orderNumber);
                    salesOrderService.createSalesOrder(salesOrder);
                });

        snsPublishService.publishMigrationOrderCreated(orderNumber);
    }
}