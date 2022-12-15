package de.kfzteile24.salesOrderHub.services.general;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ParcelShippedServiceTest {

    @Spy
    @InjectMocks
    private ParcelShippedService parcelShippedService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @Test
    void testHandleParcelShipped() {
        var message = getObjectByResource("parcelShipped.json", ParcelShippedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));

        ParcelShipped parcelShipped = message.getMessage();
        when(salesOrderService.getOrderByOrderGroupId(parcelShipped.getOrderNumber())).thenReturn(List.of(salesOrder));

        parcelShippedService.handleParcelShipped(message, messageWrapper);

        verify(salesOrderService).save(argThat(so -> so.getLatestJson().getOrderRows().stream()
                        .filter(row -> row.getSku().equals("sku-1"))
                        .allMatch(row -> row.getShippingProvider().equals("DPD"))),
                eq(ORDER_ITEM_SHIPPED));
        verify(parcelShippedService).handleParcelShippedEvent(message.getMessage(), List.of(salesOrder));
    }

    @Test
    void testHandleParcelShippedWithNoOrders() {
        var message = getObjectByResource("parcelShipped.json", ParcelShippedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        ParcelShipped parcelShipped = message.getMessage();
        when(salesOrderService.getOrderByOrderGroupId(parcelShipped.getOrderNumber())).thenReturn(List.of());

        assertThatThrownBy(() -> parcelShippedService.handleParcelShipped(message, messageWrapper))
                .isExactlyInstanceOf(NotFoundException.class)
                .hasMessageContaining(MessageFormat.format(
                        "There is no sales order for order group id {0}", message.getMessage().getOrderNumber()));

        verify(salesOrderService, never()).save(any(), any());
        verify(parcelShippedService, never()).handleParcelShippedEvent(eq(message.getMessage()), any());
    }

    @Test
    void testHandleParcelShippedEvent() {
        final SalesOrder salesOrder1 = createSalesOrder(
                UUID.randomUUID().toString(),
                null,
                "sku1"
        );
        final SalesOrder salesOrder2 = createSalesOrder(
                UUID.randomUUID().toString(),
                LocalDateTime.of(2022, 2, 1, 1, 0, 0),
                "sku1", "sku2", "sku3"
        );
        final SalesOrder salesOrder3 = createSalesOrder(
                UUID.randomUUID().toString(),
                LocalDateTime.of(2022, 3, 1, 1, 0, 0),
                "sku4", "sku5"
        );
        var orderNumber = salesOrder1.getOrderNumber();
        var event = ParcelShipped.builder()
                .orderNumber(orderNumber)
                .deliveryNoteNumber("delivery-note-12345")
                .trackingNumber("tracking-12345")
                .trackingLink("http://tacking-link")
                .logisticsPartnerName("dhl")
                .articleItemsDtos(Collections.singleton(
                        ArticleItemsDto.builder()
                                .number("sku1")
                                .quantity(BigDecimal.ONE)
                                .description("sku name 1")
                                .isDeposit(false)
                                .build()
                ))
                .build();
        List<SalesOrder> salesOrders = List.of(salesOrder3, salesOrder2, salesOrder1);

        parcelShippedService.handleParcelShippedEvent(event, salesOrders);

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder2, getTrackingLinks(event));
    }

    @Test
    @DisplayName("When parcel shipped event has combined items, then it should be ignored")
    void whenParcelShippedEventHasCombinedItemsThenItShouldBeIgnored() {
        final SalesOrder salesOrder1 = createSalesOrder(
                UUID.randomUUID().toString(),
                null,
                "sku1"
        );
        var orderNumber = salesOrder1.getOrderNumber();
        var event = ParcelShipped.builder()
                .orderNumber(orderNumber)
                .trackingLink("http://tacking-link")
                .articleItemsDtos(Collections.singleton(
                        ArticleItemsDto.builder()
                                .number("sku1")
                                .isDeposit(false)
                                .build()
                ))
                .build();

        List<SalesOrder> salesOrders = List.of(salesOrder1);

        parcelShippedService.handleParcelShippedEvent(event, salesOrders);

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder1, getTrackingLinks(event));

        event.getArticleItemsDtos().iterator().next().setNumber("sku1,sku2");

        parcelShippedService.handleParcelShippedEvent(event, salesOrders);

        verify(snsPublishService, never()).publishSalesOrderShipmentConfirmedEvent(salesOrder1, getTrackingLinks(event));
    }

    private List<TrackingLink> getTrackingLinks(ParcelShipped event) {
        return List.of(TrackingLink.builder()
                .url(event.getTrackingLink())
                .orderItems(event.getArticleItemsDtos().stream().map(ArticleItemsDto::getNumber).collect(toList()))
                .build());
    }

}
