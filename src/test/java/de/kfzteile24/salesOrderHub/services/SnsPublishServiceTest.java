package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.AwsSnsConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author vinaya
 */
@ExtendWith(MockitoExtension.class)
public class SnsPublishServiceTest {

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
    public void testPublishOrderCreatedPublishesBothOrderCreatedEventsIfTheOriginalOrderJsonIsV21() {
        final var expectedTopic1 = "order-created";
        final var expectedSubject1 = "Sales order created";
        final var expectedTopic2 = "order-created-v2";
        final var expectedSubject2 = "Sales order created V2";
        String rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = SalesOrderUtil.getSalesOrder(rawMessage);

        //given
        var orderNumber = salesOrder.getOrderNumber();
        given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.of(salesOrder));
        given(awsSnsConfig.getSnsOrderCreatedTopic()).willReturn(expectedTopic1);
        given(awsSnsConfig.getSnsOrderCreatedTopicV2()).willReturn(expectedTopic2);

        //when
        snsPublishService.publishOrderCreated(orderNumber);

        //then
        final var expectedSalesOrderInfoV1 = SalesOrderInfo.builder()
                .recurringOrder(Boolean.TRUE)
                .order(salesOrder.getOriginalOrder())
                .build();
        verify(notificationMessagingTemplate).sendNotification(expectedTopic1,
                objectMapper.writeValueAsString(expectedSalesOrderInfoV1), expectedSubject1);
        final var expectedSalesOrderInfoV2 = SalesOrderInfo.builder()
                .recurringOrder(Boolean.TRUE)
                .order(salesOrder.getLatestJson())
                .build();
        verify(notificationMessagingTemplate).sendNotification(expectedTopic2,
                objectMapper.writeValueAsString(expectedSalesOrderInfoV2), expectedSubject2);
    }


    @Test
    @SneakyThrows(Exception.class)
    public void testPublishOrderCreatedPublishesOnlyTheOrderCreatedV2EventIfTheOriginalOrderJsonIsV3() {
        final var expectedTopic2 = "order-created-v2";
        final var expectedSubject2 = "Sales order created V2";
        String rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = SalesOrderUtil.getSalesOrder(rawMessage);
        salesOrder.setOriginalOrder(salesOrder.getLatestJson());

        //given
        var orderNumber = salesOrder.getOrderNumber();
        given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.of(salesOrder));
        given(awsSnsConfig.getSnsOrderCreatedTopicV2()).willReturn(expectedTopic2);

        //when
        snsPublishService.publishOrderCreated(orderNumber);

        //then
        final var expectedSalesOrderInfoV2 = SalesOrderInfo.builder()
                .recurringOrder(Boolean.TRUE)
                .order(salesOrder.getLatestJson())
                .build();
        verify(notificationMessagingTemplate).sendNotification(expectedTopic2,
                objectMapper.writeValueAsString(expectedSalesOrderInfoV2), expectedSubject2);
        verifyNoMoreInteractions(notificationMessagingTemplate);
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testSendOrderWhenSalesOrderNotFound() {
        String rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");

        var orderNumber = "514000018";
        var snsTopic = "testsnstopic";
        var subject = "testsubject";
        SalesOrderInfo salesOrderInfo = SalesOrderInfo.builder()
                .recurringOrder(Boolean.TRUE)
                .order(SalesOrderUtil.getOrderJson(rawMessage))
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
    public void testPublishOrderCreated() {
        final var expectedTopic = "order-created";
        final var expectedSubject = "Sales order created";

        given(awsSnsConfig.getSnsOrderCreatedTopic()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishOrderCreated), false);
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testPublishOrderCancelled() {
        final var expectedTopic = "order-canceled";
        final var expectedSubject = "Sales order cancelled";

        given(awsSnsConfig.getSnsOrderCancelledTopic()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishOrderCancelled), true);
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testPublishOrderItemCancelled() {
        final var expectedTopic = "order-item canceled";
        final var expectedSubject = "Sales order item cancelled";

        given(awsSnsConfig.getSnsOrderItemCancelledTopic()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishOrderItemCancelled), true);
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testPublishOrderCompleted() {
        final var expectedTopic = "order-completed";
        final var expectedSubject = "Sales order completed";

        given(awsSnsConfig.getSnsOrderCompletedTopic()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishOrderCompleted), true);
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testPublishDeliveryAddressChanged() {
        final var expectedTopic = "delivery-address changed";
        final var expectedSubject = "Sales order delivery address changed";

        given(awsSnsConfig.getSnsDeliveryAddressChanged()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishDeliveryAddressChanged), true);
    }

    @Test
    @SneakyThrows(Exception.class)
    public void testPublishInvoiceAddressChanged() {
        final var expectedTopic = "invoice-address changed";
        final var expectedSubject = "Sales order invoice address changed";

        given(awsSnsConfig.getSnsInvoiceAddressChangedTopic()).willReturn(expectedTopic);

        verifyPublishedEvent(expectedTopic, expectedSubject,
                throwingConsumerWrapper(snsPublishService::publishInvoiceAddressChanged), true);
    }

    @SneakyThrows(Exception.class)
    private void verifyPublishedEvent(String expectedTopic, String expectedSubject, Consumer<String> executor,
                                      boolean expectLatestOrderJSON) {
        final String rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");
        final SalesOrder salesOrder = SalesOrderUtil.getSalesOrder(rawMessage);
        given(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .willReturn(Optional.of(salesOrder));

        executor.accept(salesOrder.getOrderNumber());

        var expectedSalesOrderInfo = SalesOrderInfo.builder()
                .recurringOrder(Boolean.TRUE)
                .order(expectLatestOrderJSON ? salesOrder.getLatestJson() : salesOrder.getOriginalOrder())
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