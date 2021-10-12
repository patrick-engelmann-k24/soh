package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.dto.events.OrderRowsCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInfoEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnsPublishService {

    @NonNull
    private final NotificationMessagingTemplate notificationMessagingTemplate;
    @NonNull
    private final SalesOrderService salesOrderService;
    @NonNull
    private final ObjectMapper objectMapper;
    @NonNull
    private final AwsSnsConfig config;

    public void publishOrderCreated(String orderNumber) {
        sendLatestOrderJson(config.getSnsOrderCreatedTopicV2(), "Sales order created V2", orderNumber);
    }

    public void publishInvoiceAddressChanged(String orderNumber) {
        sendLatestOrderJson(config.getSnsInvoiceAddressChangedTopic(),
                "Sales order invoice address changed", orderNumber);
    }

    public void publishDeliveryAddressChanged(String orderNumber) {
        sendLatestOrderJson(config.getSnsDeliveryAddressChanged(), "Sales order delivery address changed", orderNumber);
    }

    public void publishOrderRowsCancelled(Order order, List<OrderRows> cancelledRows, boolean isFullCancellation) {
        final var orderRowsCancelled = OrderRowsCancelledEvent.builder()
                .cancelledRows(cancelledRows)
                .order(order)
                .isFullCancellation(isFullCancellation)
                .cancellationDate(OffsetDateTime.now())
                .build();

        publishEvent(config.getSnsOrderRowsCancelledTopic(), "Sales order rows cancelled",
                orderRowsCancelled, order.getOrderHeader().getOrderNumber());
    }

    public void publishOrderCompleted(String orderNumber) {
        sendLatestOrderJson(config.getSnsOrderCompletedTopic(), "Sales order completed", orderNumber);
    }

    protected void sendLatestOrderJson(String topic, String subject, String orderNumber) {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final var salesOrderInfo = SalesOrderInfoEvent.builder()
                .order(salesOrder.getLatestJson())
                .recurringOrder(salesOrder.isRecurringOrder())
                .build();

        publishEvent(topic, subject, salesOrderInfo, orderNumber);
    }

    @SneakyThrows({JsonProcessingException.class})
    private void publishEvent(String topic, String subject, Object event, String orderNumber) {
        log.info("Publishing SNS-Topic: {} for order number {}", topic, orderNumber);

        notificationMessagingTemplate.sendNotification(topic,
                objectMapper.writeValueAsString(event), subject);
    }
}
