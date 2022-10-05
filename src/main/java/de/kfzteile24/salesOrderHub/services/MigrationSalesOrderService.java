package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import de.kfzteile24.soh.order.dto.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
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

    private final ObjectMapper objectMapper;

    private final MessageWrapperUtil messageWrapperUtil;

    @Transactional
    public void handleMigrationCoreSalesOrderCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) throws JsonProcessingException {

        var messageWrapper = messageWrapperUtil.create(rawMessage, Order.class);
        var order = messageWrapper.getMessage();
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        Order originalOrder = objectMapper.readValue(body, Order.class);
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