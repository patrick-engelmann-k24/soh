package de.kfzteile24.salesOrderHub.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;

/**
 * @author vinaya
 */
@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class SnsPublishServiceTest {

  @Mock
  private NotificationMessagingTemplate notificationMessagingTemplate;
  @Mock
  private SalesOrderService salesOrderService;
  @Spy
  private ObjectMapper objectMapper;
  @Captor
  ArgumentCaptor<String> salesOrderArgumentCaptor;
  @InjectMocks
  private SnsPublishService snsPublishService;

  @Test
  @SneakyThrows(Exception.class)
  public void testSendOrder() {
    String rawMessage = SalesOrderUtil.readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = SalesOrderUtil.getSaleOrder(rawMessage);
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
    assertThat(order.getOrder().getOrderRows().size()).isEqualTo(2);
    assertThat(order.getOrder().getLogisticalUnits().size()).isEqualTo(1);
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
