package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

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
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        if (salesOrder.getOriginalOrder() instanceof OrderJSON) {
            final var salesOrderInfo = SalesOrderInfo.builder()
                    .order(salesOrder.getOriginalOrder())
                    .recurringOrder(salesOrder.isRecurringOrder())
                    .build();
            sendOrder(config.getSnsOrderCreatedTopic(), "Sales order created", salesOrderInfo, orderNumber);
        }
        publishOrderCreatedWithLatestOrderJson(salesOrder, orderNumber);
    }

    private void publishOrderCreatedWithLatestOrderJson(SalesOrder salesOrder, String orderNumber) {
        final var salesOrderInfoV2 = SalesOrderInfo.builder()
                .order(salesOrder.getLatestJson())
                .recurringOrder(salesOrder.isRecurringOrder())
                .build();
        sendOrder(config.getSnsOrderCreatedTopicV2(), "Sales order created V2", salesOrderInfoV2, orderNumber);
    }

    public void publishInvoiceAddressChanged(String orderNumber) {
        sendLatestOrderJson(config.getSnsInvoiceAddressChangedTopic(),
                "Sales order invoice address changed", orderNumber);
    }

    public void publishDeliveryAddressChanged(String orderNumber) {
        sendLatestOrderJson(config.getSnsDeliveryAddressChanged(), "Sales order delivery address changed", orderNumber);
    }

    public void publishOrderItemCancelled(String orderNumber) {
        sendLatestOrderJson(config.getSnsOrderItemCancelledTopic(), "Sales order item cancelled", orderNumber);
    }

    public void publishOrderCancelled(String orderNumber) {
        sendLatestOrderJson(config.getSnsOrderCancelledTopic(), "Sales order cancelled", orderNumber);
    }

    public void publishOrderCompleted(String orderNumber) {
        sendLatestOrderJson(config.getSnsOrderCompletedTopic(), "Sales order completed", orderNumber);
    }

    protected void sendLatestOrderJson(String topic, String subject, String orderNumber) {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final var salesOrderInfo = SalesOrderInfo.builder()
                .order(salesOrder.getLatestJson())
                .recurringOrder(salesOrder.isRecurringOrder())
                .build();

        sendOrder(topic, subject, salesOrderInfo, orderNumber);
    }

    @SneakyThrows({JsonProcessingException.class})
    private void sendOrder(String topic, String subject, SalesOrderInfo salesOrderInfo, String orderNumber) {
        log.info("Publishing SNS-Topic: {} for order number {}", topic, orderNumber);

        notificationMessagingTemplate.sendNotification(topic,
                objectMapper.writeValueAsString( salesOrderInfo), subject);
    }
}
