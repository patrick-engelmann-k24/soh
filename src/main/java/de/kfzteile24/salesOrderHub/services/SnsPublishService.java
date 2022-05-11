package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.OrderCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.OrderRowCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.ReturnOrderCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderCompletedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInfoEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInvoiceCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderShipmentConfirmedEvent;
import de.kfzteile24.salesOrderHub.dto.events.CoreSalesInvoiceCreatedReceivedEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
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

    public void publishOrderCancelled(Order order) {
        final var orderCancelled = OrderCancelledEvent.builder()
                .order(order)
                .build();

        publishEvent(config.getSnsSalesOrderCancelled(), "Sales order cancelled",
                orderCancelled, order.getOrderHeader().getOrderNumber());
    }

    public void publishOrderCompleted(String orderNumber) {
        final var salesOrderCompleted = SalesOrderCompletedEvent.builder().orderNumber(orderNumber).build();
        publishEvent(config.getSnsOrderCompletedTopic(), "Sales order completed", salesOrderCompleted, orderNumber);
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

    public void publishCoreInvoiceReceivedEvent(CoreSalesInvoiceCreatedReceivedEvent event) {
        var orderNumber = event.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();

        publishEvent(config.getSnsCoreInvoiceReceivedV1(), "Core Sales Invoice Received V1",
                event, orderNumber);
    }

    public void publishSalesOrderShipmentConfirmedEvent(SalesOrder salesOrder, Collection<String> trackingLinks) {

        var salesOrderShipmentConfirmedEvent = SalesOrderShipmentConfirmedEvent.builder()
                .order(salesOrder.getLatestJson())
                .trackingLinks(trackingLinks)
                .build();

        publishEvent(config.getSnsShipmentConfirmedV1(), "Sales order shipment confirmed V1",
                salesOrderShipmentConfirmedEvent, salesOrder.getOrderNumber());

    }

    public void publishReturnOrderCreatedEvent(SalesOrderReturn salesOrderReturn) {
        var returnOrderCreatedEvent = ReturnOrderCreatedEvent.builder()
                .order(salesOrderReturn.getReturnOrderJson())
                .build();

        publishEvent(config.getSnsReturnOrderCreatedV1(), "Return Order Created V1",
                returnOrderCreatedEvent, salesOrderReturn.getOrderNumber());
    }

    public void publishCreditNoteReceivedEvent(SalesCreditNoteReceivedEvent salesCreditNoteReceivedEvent) {
        var orderNumber =
                salesCreditNoteReceivedEvent.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        publishEvent(config.getSnsCreditNoteReceivedV1(), "Credit Note Received V1",
                salesCreditNoteReceivedEvent, orderNumber);
    }

    public void publishMigrationOrderCreated(String orderNumber) {
        sendLatestOrderJson(config.getSnsMigrationOrderCreatedV2(), "Migration Sales order created V2", orderNumber);
    }

    public void publishMigrationOrderRowCancelled(String orderNumber, String orderRowId) {
        final var orderRowCancelled = OrderRowCancelledEvent.builder()
                .orderNumber(orderNumber)
                .orderRowNumber(orderRowId)
                .build();

        publishEvent(config.getSnsMigrationSalesOrderRowCancelledV1(), "Sales order row cancelled",
                orderRowCancelled, orderNumber);
    }

    public void publishMigrationOrderCancelled(Order order) {
        final var orderCancelled = OrderCancelledEvent.builder()
                .order(order)
                .build();

        publishEvent(config.getSnsMigrationSalesOrderCancelledV1(), "Sales order cancelled",
                orderCancelled, order.getOrderHeader().getOrderNumber());
    }

    public void publishMigrationReturnOrderCreatedEvent(SalesOrderReturn salesOrderReturn) {
        var returnOrderCreatedEvent = ReturnOrderCreatedEvent.builder()
                .order(salesOrderReturn.getReturnOrderJson())
                .build();

        publishEvent(config.getSnsMigrationReturnOrderCreatedV1(), "Return Order Created V1",
                returnOrderCreatedEvent, salesOrderReturn.getOrderNumber());
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
