package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.events.OrderRowCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInfoEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInvoiceCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderShipmentConfirmedEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

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

    public void publishOrderRowCancelled(String orderNumber, String orderRowId) {
        final var orderRowCancelled = OrderRowCancelledEvent.builder()
                .orderNumber(orderNumber)
                .orderRowNumber(orderRowId)
                .build();

        publishEvent(config.getSnsSalesOrderRowCancelled(), "Sales order row cancelled",
                orderRowCancelled, orderNumber);
    }

    public void publishOrderCancelled(String orderNumber) {
        final var orderCancelled = OrderRowCancelledEvent.builder()
                .orderNumber(orderNumber)
                .build();

        publishEvent(config.getSnsSalesOrderCancelled(), "Sales order cancelled",
                orderCancelled, orderNumber);
    }

    public void publishOrderCompleted(String orderNumber) {
        sendLatestOrderJson(config.getSnsOrderCompletedTopic(), "Sales order completed", orderNumber);
    }

    public void publishOrderInvoiceCreated(String orderNumber, String invoiceUrl) {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final var salesOrderInvoiceCreatedEvent = SalesOrderInvoiceCreatedEvent.builder()
                .order(salesOrder.getLatestJson())
                .invoiceDocumentLink(invoiceUrl)
                .build();

        publishEvent(config.getSnsOrderInvoiceCreatedV1(), "Sales order invoice created V1",
                salesOrderInvoiceCreatedEvent, orderNumber);
    }

    public void publishSalesOrderShipmentConfirmedEvent(SalesOrder salesOrder, Collection<String> trackingLinks) {

        var salesOrderShipmentConfirmedEvent = SalesOrderShipmentConfirmedEvent.builder()
                .order(salesOrder.getLatestJson())
                .trackingLinks(trackingLinks)
                .build();

        publishEvent(config.getSnsShipmentConfirmedV1(), "Sales order shipment confirmed V1",
                salesOrderShipmentConfirmedEvent, salesOrder.getOrderNumber());

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
