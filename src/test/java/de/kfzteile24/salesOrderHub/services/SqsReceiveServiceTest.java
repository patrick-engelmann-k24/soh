package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.converter.OrderJsonConverter;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.converter.OrderJsonVersionDetector;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
public class SqsReceiveServiceTest {

  @Spy
  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
  @Mock
  private RuntimeService runtimeService;
  @Mock
  private SalesOrderService salesOrderService;
  @Mock
  private CamundaHelper camundaHelper;
  @Mock
  private OrderJsonConverter orderJsonConverter;
  @Mock
  private OrderJsonVersionDetector orderJsonVersionDetector;
  @Captor
  private ArgumentCaptor<SalesOrder> salesOrderArgumentCaptor;
  @InjectMocks
  private SqsReceiveService sqsReceiveService;

  @Test
  public void testQueueListenerEcpShopOrdersWhenRecurringOrderReceived() throws Exception {
    var senderId = "Ecp";
    String rawMessage =  readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = getSalesOrder(rawMessage);
    salesOrder.setRecurringOrder(false);

    when(orderJsonConverter.convert(any())).thenReturn(salesOrder.getLatestJson());

    when(salesOrderService.isRecurringOrder(salesOrder)).thenReturn(true);

    when(orderJsonVersionDetector.isVersion2(any())).thenReturn(true);

    sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId);

    verify(salesOrderService).save(salesOrderArgumentCaptor.capture(), eq(ORDER_CREATED));

    final var actualSalesOrder = salesOrderArgumentCaptor.getValue();
    assertThat(actualSalesOrder.isRecurringOrder()).isTrue();
    assertThat(actualSalesOrder.getLatestJson()).isNotNull();
    assertThat(actualSalesOrder.getCustomerEmail()).isEqualTo("test@kfzteile24.de");
    assertThat(actualSalesOrder.getSalesChannel()).isEqualTo("www-k24-at");
    assertThat(actualSalesOrder.getOrderNumber()).isEqualTo("514000016");

    verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
    verify(orderJsonConverter).convert(eq((OrderJSON) salesOrder.getOriginalOrder()));
  }

  @Test
  public void testQueueListenerEcpShopOrdersWhenNewOrderReceived() throws JsonProcessingException {
    var senderId = "Ecp";
    String rawMessage =  readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = getSalesOrder(rawMessage);
    salesOrder.setRecurringOrder(false);

    when(orderJsonConverter.convert(any())).thenReturn(salesOrder.getLatestJson());

    when(salesOrderService.isRecurringOrder(salesOrder)).thenReturn(false);

    when(orderJsonVersionDetector.isVersion2(any())).thenReturn(true);

    sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId);

    verify(salesOrderService).save(salesOrderArgumentCaptor.capture(), eq(ORDER_CREATED));
    assertThat(salesOrderArgumentCaptor.getValue().isRecurringOrder()).isFalse();
    verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
    verify(orderJsonConverter).convert(eq((OrderJSON) salesOrder.getOriginalOrder()));
  }

  @SneakyThrows({URISyntaxException.class, IOException.class})
  private String readResource(String path) {
    return java.nio.file.Files.readString(Paths.get(
            Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                    .toURI()));
  }

}
