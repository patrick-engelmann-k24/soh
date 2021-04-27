package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
  @Captor
  ArgumentCaptor<String> salesOrderArgumentCaptor;
  @InjectMocks
  private SnsPublishService snsPublishService;

  @Test
  @SneakyThrows(Exception.class)
  public void testSendOrder() {
    String rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = SalesOrderUtil.getSalesOrder(rawMessage);
    //given
    var orderNumber = salesOrder.getOrderNumber();
    var snsTopic = "testsnstopic";
    var subject = "publishorder";
    SalesOrderInfo salesOrderInfo = SalesOrderInfo.builder()
                                                                  .recurringOrder(Boolean.TRUE)
                                                                  .order(SalesOrderUtil.getOrderJson(rawMessage))
                                                                  .build();
    given(salesOrderService.getOrderByOrderNumber(orderNumber)).willReturn(Optional.of(salesOrder));
    //when
    snsPublishService.sendOrder(snsTopic, subject, orderNumber);
    //then
    verify(notificationMessagingTemplate, times(1)).sendNotification(snsTopic,
                                                                                            objectMapper.writeValueAsString(
                                                                                                salesOrderInfo), subject);
    verify(notificationMessagingTemplate, times(1)).sendNotification(any(),
                                                                                            salesOrderArgumentCaptor.capture(),
                                                                                            any());
    SalesOrderInfo order = objectMapper.readValue(salesOrderArgumentCaptor.getValue(),
                                                       SalesOrderInfo.class);
    assertThat(order.isRecurringOrder()).isTrue();
    assertThat(order.getOrder().getVersion()).isEqualTo("2.1");
    assertThat(order.getOrder().getOrderHeader().getOrderNumber()).isEqualTo("514000016");
    assertThat(order.getOrder().getOrderHeader().getOrderDatetime()).isEqualTo("2021-03-16T09:54:10.162Z");
    assertThat(order.getOrder().getOrderRows().size()).isEqualTo(2);
    assertThat(order.getOrder().getLogisticalUnits().size()).isEqualTo(1);
  }

  @Test
  @SneakyThrows(Exception.class)
  public void testThatTheLatestOrderJsonIsPublished() {
    final var rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");
    final var salesOrder = SalesOrderUtil.getSalesOrder(rawMessage);
    final var salesOrderLatest = SalesOrderUtil.getSalesOrder(rawMessage);

    salesOrderLatest.getLatestJson().getOrderHeader().getBillingAddress().setCity("Berlin");
    salesOrder.setLatestJson(salesOrderLatest.getLatestJson());
    assertThat(salesOrder.getOriginalOrder().getOrderHeader().getBillingAddress().getCity())
            .isNotEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getCity());

    given(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()))
            .willReturn(Optional.of(salesOrder));

    snsPublishService.sendOrder("", "", salesOrder.getOrderNumber());

    verify(notificationMessagingTemplate).sendNotification(any(), salesOrderArgumentCaptor.capture(), any());

    final var salesOrderInfo = objectMapper.readValue(salesOrderArgumentCaptor.getValue(),
            SalesOrderInfo.class);
    assertThat(salesOrderInfo.getOrder().getOrderHeader().getBillingAddress().getCity())
            .isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getCity());
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
    assertThatThrownBy(() -> snsPublishService.sendOrder(snsTopic, subject, orderNumber))
        .isInstanceOf(SalesOrderNotFoundException.class)
        .hasMessageContaining("Sales order not found for the given order number ", orderNumber);
    //then
    verify(notificationMessagingTemplate, never()).sendNotification(snsTopic,
        objectMapper.writeValueAsString(salesOrderInfo), subject);
  }

}
