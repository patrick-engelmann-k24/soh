package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

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

    public void publishOrderCreated(String orderNumber) throws Exception {
        sendOrder(config.getSnsOrderCreatedTopic(), "Sales order created", orderNumber);
    }

    public void publishInvoiceAddressChanged(String orderNumber) throws Exception {
        sendOrder(config.getSnsInvoiceAddressChangedTopic(), "Sales order invoice address changed", orderNumber);
    }

    public void publishDeliveryAddressChanged(String orderNumber) throws Exception {
        sendOrder(config.getSnsDeliveryAddressChanged(), "Sales order delivery address changed", orderNumber);
    }

    public void publishOrderItemCancelled(String orderNumber) throws Exception {
        sendOrder(config.getSnsOrderItemCancelledTopic(), "Sales order item cancelled", orderNumber);
    }

    public void publishOrderCancelled(String orderNumber) throws Exception {
        sendOrder(config.getSnsOrderCancelledTopic(), "Sales order cancelled", orderNumber);
    }

    public void publishOrderCompleted(String orderNumber) throws Exception {
        sendOrder(config.getSnsOrderCompletedTopic(), "Sales order completed", orderNumber);
    }

    @SneakyThrows({JsonProcessingException.class})
    void sendOrder(String topic, String subject, String orderNumber) throws Exception {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(MessageFormat.format(
                        "Sales order not found for the given order number {0} ", orderNumber)));

        final var salesOrderInfo = SalesOrderInfo.builder()
                .order(salesOrder.getLatestJson())
                .recurringOrder(salesOrder.isRecurringOrder())
                .build();

        log.info("Publishing SNS-Topic: {} for order number{}", topic, orderNumber);
        notificationMessagingTemplate.sendNotification(topic,
                objectMapper.writeValueAsString(
                        salesOrderInfo),
                subject);
    }
}
