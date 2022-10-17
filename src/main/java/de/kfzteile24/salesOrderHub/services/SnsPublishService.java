package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.constants.CustomEventName;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.CoreSalesInvoiceCreatedReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.events.DropshipmentOrderCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.DropshipmentOrderReturnNotifiedEvent;
import de.kfzteile24.salesOrderHub.dto.events.OrderCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.OrderRowCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.PayoutReceiptConfirmationReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.events.ReturnOrderCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderCompletedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInfoEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInvoiceCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.dropshipment.DropshipmentOrderPackage;
import de.kfzteile24.salesOrderHub.dto.events.dropshipment.DropshipmentOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.SalesOrderShipmentConfirmedEvent;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnsPublishService {

    private final NotificationMessagingTemplate notificationMessagingTemplate;
    private final SalesOrderService salesOrderService;
    private final ObjectMapper objectMapper;
    private final AwsSnsConfig config;
    private final MetricsHelper metricsHelper;
    private final SalesOrderReturnService salesOrderReturnService;

    public void publishOrderCreated(String orderNumber) {
        var salesOrder = sendLatestOrderJson(config.getSnsOrderCreatedTopicV2(), "Sales order created V2", orderNumber);
        metricsHelper.sendCustomEvent(salesOrder, CustomEventName.SALES_ORDER_PUBLISHED);
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
                .sku(orderRowId)
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

    public void publishSalesOrderShipmentConfirmedEvent(SalesOrder salesOrder, Collection<TrackingLink> trackingLinks) {

        var salesOrderShipmentConfirmedEvent = SalesOrderShipmentConfirmedEvent.builder()
                .order(salesOrder.getLatestJson())
                .trackingLinks(trackingLinks)
                .build();

        publishEvent(config.getSnsShipmentConfirmedV1(), "Sales order shipment confirmed V1",
                salesOrderShipmentConfirmedEvent, salesOrder.getOrderNumber());

    }

    public void publishDropshipmentOrderCreatedEvent(SalesOrder salesOrder) {

        var dropshipmentOrderCreatedEvent = DropshipmentOrderCreatedEvent.builder()
                .order(salesOrder.getLatestJson())
                .build();

        publishEvent(config.getSnsDropshipmentOrderCreatedV1(), "Dropshipment Order Created V1",
                dropshipmentOrderCreatedEvent, salesOrder.getOrderNumber());

    }

    public void publishDropshipmentOrderReturnNotifiedEvent(SalesOrder salesOrder,
                                                            DropshipmentPurchaseOrderReturnNotifiedMessage message) {

        var packages = message.getPackages().stream()
                .map(p -> DropshipmentOrderPackage.builder()
                        .trackingLink(p.getTrackingLink())
                        .items(p.getItems().stream()
                                .map(item -> DropshipmentOrderPackageItemLine.builder()
                                        .sku(item.getProductNumber())
                                        .quantity(item.getQuantity())
                                        .build()
                                ).collect(Collectors.toList()))
                        .build()).collect(Collectors.toList());

        var dropshipmentOrderReturnNotifiedEvent = DropshipmentOrderReturnNotifiedEvent.builder()
                .order(salesOrder.getLatestJson())
                .packages(packages)
                .build();

        publishEvent(config.getSnsDropshipmentOrderReturnNotifiedV1(), "Dropshipment Order Return Notified V1",
                dropshipmentOrderReturnNotifiedEvent, salesOrder.getOrderNumber());

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

    public void publishCreditNoteCreatedEvent(SalesCreditNoteCreatedEvent salesCreditNoteCreatedEvent) {
        var orderNumber = salesCreditNoteCreatedEvent.getReturnOrder().getOrderHeader().getOrderNumber();
        publishEvent(config.getSnsCreditNoteCreatedV1(), "Credit Note Created V1",
                salesCreditNoteCreatedEvent, orderNumber);
    }

    public void publishMigrationOrderCreated(String orderNumber) {
        sendLatestOrderJson(config.getSnsMigrationOrderCreatedV2(), "Migration Sales order created V2", orderNumber);
    }

    public void publishMigrationOrderRowCancelled(String orderNumber, String orderRowId) {
        final var orderRowCancelled = OrderRowCancelledEvent.builder()
                .orderNumber(orderNumber)
                .sku(orderRowId)
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

    public void publishMigrationReturnOrderCreatedEvent(String returnOrderNumber) {
        salesOrderReturnService.getByOrderNumber(returnOrderNumber)
                .ifPresentOrElse(this::publishMigrationReturnOrderCreatedEvent, () -> {
                    throw new SalesOrderNotFoundException(returnOrderNumber);
                });
    }

    public void publishPayoutReceiptConfirmationReceivedEvent(SalesOrderReturn salesOrderReturn) {
        var payoutReceiptConfirmationReceivedEvent = PayoutReceiptConfirmationReceivedEvent.builder()
                .order(salesOrderReturn.getReturnOrderJson())
                .build();

        publishEvent(config.getSnsPayoutReceiptConfirmationReceivedV1(), "Payout Receipt Confirmation Received V1",
                payoutReceiptConfirmationReceivedEvent, salesOrderReturn.getOrderNumber());
    }

    protected SalesOrder sendLatestOrderJson(String topic, String subject, String orderNumber) {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final var salesOrderInfo = SalesOrderInfoEvent.builder()
                .order(salesOrder.getLatestJson())
                .recurringOrder(salesOrder.isRecurringOrder())
                .build();

        publishEvent(topic, subject, salesOrderInfo, orderNumber);

        return salesOrder;
    }

    @SneakyThrows({JsonProcessingException.class})
    private void publishEvent(String topic, String subject, Object event, String orderNumber) {
        log.info("Publishing SNS-Topic: {} for order number {}", topic, orderNumber);

        notificationMessagingTemplate.sendNotification(topic,
                objectMapper.writeValueAsString(event), subject);
    }
}
