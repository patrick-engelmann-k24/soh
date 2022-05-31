package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.DropshipmentOrderReturnNotifiedEvent;
import de.kfzteile24.salesOrderHub.dto.events.OrderCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.OrderRowCancelledEvent;
import de.kfzteile24.salesOrderHub.dto.events.ReturnOrderCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderCompletedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInfoEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderInvoiceCreatedEvent;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderShipmentConfirmedEvent;
import de.kfzteile24.salesOrderHub.dto.events.dropshipment.DropshipmentOrderPackage;
import de.kfzteile24.salesOrderHub.dto.events.dropshipment.DropshipmentOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackage;
import de.kfzteile24.salesOrderHub.dto.sns.dropshipment.DropshipmentPurchaseOrderPackageItemLine;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */
@ExtendWith(MockitoExtension.class)
class SnsPublishServiceTest {

    @Mock
    private NotificationMessagingTemplate notificationMessagingTemplate;
    @Mock
    private SalesOrderService salesOrderService;
    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private AwsSnsConfig awsSnsConfig;

    @InjectMocks
    private SnsPublishService snsPublishService;

    @Test
    @SneakyThrows(Exception.class)
    void testPublishOrderCreatedPublishesBothOrderCreatedEventsIfTheOriginalOrderJsonIsV21() {
        final var expectedTopic2 = "order-created-v2";
        final var expectedSubject2 = "Sales order created V2";
        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);

        //given
        var orderNumber = salesOrder.getOrderNumber();
        given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.of(salesOrder));
        given(awsSnsConfig.getSnsOrderCreatedTopicV2()).willReturn(expectedTopic2);

        //when
        snsPublishService.publishOrderCreated(orderNumber);

        //then
        final var expectedSalesOrderInfoV2 = SalesOrderInfoEvent.builder()
                .recurringOrder(Boolean.TRUE)
                .order(salesOrder.getLatestJson())
                .build();
        verify(notificationMessagingTemplate).sendNotification(expectedTopic2,
                objectMapper.writeValueAsString(expectedSalesOrderInfoV2), expectedSubject2);
    }


    @Test
    @SneakyThrows(Exception.class)
    void testPublishOrderCreatedPublishesOnlyTheOrderCreatedV2EventIfTheOriginalOrderJsonIsV3() {
        final var expectedTopic2 = "order-created-v2";
        final var expectedSubject2 = "Sales order created V2";
        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setOriginalOrder(salesOrder.getLatestJson());

        //given
        var orderNumber = salesOrder.getOrderNumber();
        given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.of(salesOrder));
        given(awsSnsConfig.getSnsOrderCreatedTopicV2()).willReturn(expectedTopic2);

        //when
        snsPublishService.publishOrderCreated(orderNumber);

        //then
        final var expectedSalesOrderInfoV2 = SalesOrderInfoEvent.builder()
                .recurringOrder(Boolean.TRUE)
                .order(salesOrder.getLatestJson())
                .build();
        verify(notificationMessagingTemplate).sendNotification(expectedTopic2,
                objectMapper.writeValueAsString(expectedSalesOrderInfoV2), expectedSubject2);
        verifyNoMoreInteractions(notificationMessagingTemplate);
    }

    @Test
    @SneakyThrows(Exception.class)
    void testSendOrderWhenSalesOrderNotFound() {
        String rawMessage = readResource("examples/ecpOrderMessage.json");

        var orderNumber = "514000018";
        var snsTopic = "testsnstopic";
        var subject = "testsubject";
        SalesOrderInfoEvent salesOrderInfo = SalesOrderInfoEvent.builder()
                .recurringOrder(Boolean.TRUE)
                .order(SalesOrderUtil.getOrder(rawMessage))
                .build();

        //given
        given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.empty());
        //when
        assertThatThrownBy(() -> snsPublishService.sendLatestOrderJson(snsTopic, subject, orderNumber))
                .isInstanceOf(SalesOrderNotFoundException.class)
                .hasMessageContaining("Sales order not found for the given order number ", orderNumber);
        //then
        verify(notificationMessagingTemplate, never()).sendNotification(snsTopic,
            objectMapper.writeValueAsString(salesOrderInfo), subject);
   }

    @Test
    @SneakyThrows(Exception.class)
    void testPublishOrderRowsCancelled() {
        final var expectedTopic = "order-row-cancelled";
        final var expectedSubject = "Sales order row cancelled";

        given(awsSnsConfig.getSnsSalesOrderRowCancelled()).willReturn(expectedTopic);

        final var latestOrderJson = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW).getLatestJson();

        final OrderRows canceledOrderRow = latestOrderJson.getOrderRows().get(0);

        snsPublishService.publishOrderRowCancelled(latestOrderJson.getOrderHeader().getOrderNumber(), canceledOrderRow.getSku());

        var expectedEvent = OrderRowCancelledEvent.builder()
                .orderNumber(latestOrderJson.getOrderHeader().getOrderNumber())
                .sku(canceledOrderRow.getSku())
                .build();

        verify(notificationMessagingTemplate).sendNotification(
                eq(expectedTopic),
                argThat(json -> {
                    try {
                        final var publishedEvent = objectMapper.readValue(((String) json), OrderRowCancelledEvent.class);

                        assertEquals(expectedEvent.getOrderNumber(), publishedEvent.getOrderNumber());
                        assertEquals(expectedEvent.getSku(), publishedEvent.getSku());
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(e);
                    }
                    return true;
                }),
                eq(expectedSubject));
    }

    @Test
    @SneakyThrows(Exception.class)
    void testPublishOrderCompleted() {
        final var expectedTopic = "soh-sales-order-completed-v1";
        final var expectedSubject = "Sales order completed";
        final var expectedEvent = SalesOrderCompletedEvent.builder()
                .orderNumber("123456789")
                .build();

        given(awsSnsConfig.getSnsOrderCompletedTopic()).willReturn(expectedTopic);

        snsPublishService.publishOrderCompleted("123456789");

        verify(notificationMessagingTemplate).sendNotification(
                eq(expectedTopic),
                argThat(json -> {
                    try {
                        final var publishedEvent = objectMapper.readValue(((String) json), SalesOrderCompletedEvent.class);
                        assertEquals(expectedEvent.getOrderNumber(), publishedEvent.getOrderNumber());
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(e);
                    }
                    return true;
                }),
                eq(expectedSubject));
    }

    @Test
    @SneakyThrows(Exception.class)
    void testPublishDeliveryAddressChanged() {
        final var expectedTopic = "delivery-address changed";
        final var expectedSubject = "Sales order delivery address changed";

        given(awsSnsConfig.getSnsDeliveryAddressChanged()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishDeliveryAddressChanged));
    }

    @Test
    @SneakyThrows(Exception.class)
    void testPublishInvoiceAddressChanged() {
        final var expectedTopic = "invoice-address changed";
        final var expectedSubject = "Sales order invoice address changed";

        given(awsSnsConfig.getSnsInvoiceAddressChangedTopic()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishInvoiceAddressChanged));
    }

    @Test
    @SneakyThrows
    void testPublishOrderInvoiceCreated() {
        final var expectedTopic = "order-invoice-created";
        final var expectedSubject = "Sales order invoice created V1";

        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        final var invoiceUrl = "s3://production-k24-invoices/dropshipment/2021/06/04/xxxxxxxxx-xxxxxxxxx.pdf";

        when(awsSnsConfig.getSnsOrderInvoiceCreatedV1()).thenReturn(expectedTopic);
        when(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())).thenReturn(Optional.of(salesOrder));

        final var expectedSalesOrderInvoiceCreatedEvent = SalesOrderInvoiceCreatedEvent.builder()
                .order(salesOrder.getLatestJson())
                .invoiceDocumentLink(invoiceUrl)
                .build();

        snsPublishService.publishOrderInvoiceCreated(salesOrder.getOrderNumber(), invoiceUrl);

        verify(notificationMessagingTemplate).sendNotification(
                expectedTopic,
                objectMapper.writeValueAsString(expectedSalesOrderInvoiceCreatedEvent),
                expectedSubject
        );
    }

    @Test
    @SneakyThrows
    void testPublishShipmentConfirmed() {
        final var expectedTopic = "shipment-confirmed-created";
        final var expectedSubject = "Sales order shipment confirmed V1";

        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        final var trackingLinks = List.of(
                "http://abc1",
                "http://abc2");

        when(awsSnsConfig.getSnsShipmentConfirmedV1()).thenReturn(expectedTopic);

        final var salesOrderShipmentConfirmedEvent = SalesOrderShipmentConfirmedEvent.builder()
                .order(salesOrder.getLatestJson())
                .trackingLinks(trackingLinks)
                .build();

        snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, trackingLinks);

        verify(notificationMessagingTemplate).sendNotification(
                expectedTopic,
                objectMapper.writeValueAsString(salesOrderShipmentConfirmedEvent),
                expectedSubject
        );
    }

    @Test
    @SneakyThrows
    void testPublishReturnOrderCreated() {
        final var expectedTopic = "return-order-created";
        final var expectedSubject = "Return Order Created V1";

        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);

        final var salesOrderReturn = SalesOrderReturn.builder()
                .returnOrderJson(salesOrder.getLatestJson())
                .orderNumber(salesOrder.getOrderNumber())
                .build();

        when(awsSnsConfig.getSnsReturnOrderCreatedV1()).thenReturn(expectedTopic);

        final var returnOrderCreatedEvent = ReturnOrderCreatedEvent.builder()
                .order(salesOrderReturn.getReturnOrderJson())
                .build();

        snsPublishService.publishReturnOrderCreatedEvent(salesOrderReturn);

        verify(notificationMessagingTemplate).sendNotification(
                expectedTopic,
                objectMapper.writeValueAsString(returnOrderCreatedEvent),
                expectedSubject
        );
    }

    @Test
    @SneakyThrows
    void testPublishMigrationOrderCreated() {

        final var expectedTopic = "migration-soh-order-created-v2";
        final var expectedSubject = "Migration Sales order created V2";

        SalesOrder salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        var orderNumber = salesOrder.getOrderNumber();
        given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.of(salesOrder));
        given(awsSnsConfig.getSnsMigrationOrderCreatedV2()).willReturn(expectedTopic);

        snsPublishService.publishMigrationOrderCreated(orderNumber);

        final var expectedMigrationSalesOrderInfoV2 = SalesOrderInfoEvent.builder()
                .recurringOrder(salesOrder.isRecurringOrder())
                .order(salesOrder.getLatestJson())
                .build();
        verify(notificationMessagingTemplate).sendNotification(expectedTopic,
                objectMapper.writeValueAsString(expectedMigrationSalesOrderInfoV2), expectedSubject);
    }

    @Test
    @SneakyThrows(Exception.class)
    void testPublishMigrationOrderRowCancelled() {
        final var expectedTopic = "migration-soh-sales-order-row-cancelled";
        final var expectedSubject = "Sales order row cancelled";

        given(awsSnsConfig.getSnsMigrationSalesOrderRowCancelledV1()).willReturn(expectedTopic);

        final var latestOrderJson = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW).getLatestJson();

        final OrderRows canceledOrderRow = latestOrderJson.getOrderRows().get(0);

        snsPublishService.publishMigrationOrderRowCancelled(latestOrderJson.getOrderHeader().getOrderNumber(), canceledOrderRow.getSku());

        var expectedEvent = OrderRowCancelledEvent.builder()
                .orderNumber(latestOrderJson.getOrderHeader().getOrderNumber())
                .sku(canceledOrderRow.getSku())
                .build();

        verify(notificationMessagingTemplate).sendNotification(
                eq(expectedTopic),
                argThat(json -> {
                    try {
                        final var publishedEvent = objectMapper.readValue(((String) json), OrderRowCancelledEvent.class);

                        assertEquals(expectedEvent.getOrderNumber(), publishedEvent.getOrderNumber());
                        assertEquals(expectedEvent.getSku(), publishedEvent.getSku());
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(e);
                    }
                    return true;
                }),
                eq(expectedSubject));
    }

    @Test
    @SneakyThrows(Exception.class)
    void testPublishMigrationOrderCancelled() {
        final var expectedTopic = "migration-soh-sales-order-cancelled";
        final var expectedSubject = "Sales order cancelled";

        given(awsSnsConfig.getSnsMigrationSalesOrderCancelledV1()).willReturn(expectedTopic);

        final var latestOrderJson = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW).getLatestJson();

        snsPublishService.publishMigrationOrderCancelled(latestOrderJson);

        var expectedEvent = OrderCancelledEvent.builder()
                .order(latestOrderJson)
                .build();

        verify(notificationMessagingTemplate).sendNotification(
                eq(expectedTopic),
                argThat(json -> {
                    try {
                        final var publishedEvent = objectMapper.readValue(((String) json), OrderCancelledEvent.class);

                        assertEquals(expectedEvent.getOrder(), publishedEvent.getOrder());
                    } catch (JsonProcessingException e) {
                        throw new IllegalArgumentException(e);
                    }
                    return true;
                }),
                eq(expectedSubject));
    }

    @Test
    @SneakyThrows
    void testPublishMigrationReturnOrderCreated() {
        final var expectedTopic = "migration-soh-return-order-created";
        final var expectedSubject = "Return Order Created V1";

        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);

        final var salesOrderReturn = SalesOrderReturn.builder()
                .returnOrderJson(salesOrder.getLatestJson())
                .orderNumber(salesOrder.getOrderNumber())
                .build();

        when(awsSnsConfig.getSnsMigrationReturnOrderCreatedV1()).thenReturn(expectedTopic);

        final var returnOrderCreatedEvent = ReturnOrderCreatedEvent.builder()
                .order(salesOrderReturn.getReturnOrderJson())
                .build();

        snsPublishService.publishMigrationReturnOrderCreatedEvent(salesOrderReturn);

        verify(notificationMessagingTemplate).sendNotification(
                expectedTopic,
                objectMapper.writeValueAsString(returnOrderCreatedEvent),
                expectedSubject
        );
    }

    @Test
    @SneakyThrows
    void testPublishDropshipmentOrderReturnNotifiedEvent() {
        final var expectedTopic = "dropshipment-order-return-notified";
        final var expectedSubject = "Dropshipment Order Return Notified V1";

        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        final var receivedEvent = DropshipmentPurchaseOrderReturnNotifiedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .externalOrderNumber("1234")
                .packages(List.of(DropshipmentPurchaseOrderPackage.builder()
                        .trackingLink("http://abc1")
                        .items(salesOrder.getLatestJson().getOrderRows().stream().map(row -> DropshipmentPurchaseOrderPackageItemLine.builder()
                                .productNumber(row.getSku())
                                .quantity(row.getQuantity().intValue())
                                .build()).collect(Collectors.toList()))
                        .build()))
                .build();

        when(awsSnsConfig.getSnsDropshipmentOrderReturnNotifiedV1()).thenReturn(expectedTopic);
        snsPublishService.publishDropshipmentOrderReturnNotifiedEvent(salesOrder, receivedEvent);

        final var expectedEvent = DropshipmentOrderReturnNotifiedEvent.builder()
                .order(salesOrder.getLatestJson())
                .packages(List.of(DropshipmentOrderPackage.builder()
                        .trackingLink("http://abc1")
                        .items(salesOrder.getLatestJson().getOrderRows().stream().map(row -> DropshipmentOrderPackageItemLine.builder()
                                .sku(row.getSku())
                                .quantity(row.getQuantity().intValue())
                                .build()).collect(Collectors.toList()))
                        .build()))
                .build();

        verify(notificationMessagingTemplate).sendNotification(
                expectedTopic,
                objectMapper.writeValueAsString(expectedEvent),
                expectedSubject
        );
    }

    @SneakyThrows(Exception.class)
    private void verifyPublishedEvent(String expectedTopic, String expectedSubject, Consumer<String> executor) {
        final String rawMessage = readResource("examples/ecpOrderMessage.json");
        final SalesOrder salesOrder = getSalesOrder(rawMessage);
        given(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .willReturn(Optional.of(salesOrder));

        executor.accept(salesOrder.getOrderNumber());

        var expectedSalesOrderInfo = SalesOrderInfoEvent.builder()
                .recurringOrder(Boolean.TRUE)
                .order(salesOrder.getLatestJson())
                .build();

        verify(notificationMessagingTemplate).sendNotification(
                expectedTopic,
                objectMapper.writeValueAsString(expectedSalesOrderInfo),
                expectedSubject
        );
    }

    @FunctionalInterface
    private interface ThrowingConsumer {
        void accept(String t) throws Exception;
    }

    private static Consumer<String> throwingConsumerWrapper(ThrowingConsumer throwingConsumer) {
        return orderNumber -> {
            try {
                throwingConsumer.accept(orderNumber);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        };
    }
}