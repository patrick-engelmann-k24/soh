package de.kfzteile24.salesOrderHub.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Locale;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
public class SqsReceiveServiceTest {

  @Spy
  private ObjectMapper mapper;
  @Mock
  private RuntimeService runtimeService;
  @Mock
  private SalesOrderService salesOrderService;
  @Mock
  private CamundaHelper camundaHelper;
  @Mock
  private SalesOrderRepository salesOrderRepository;
  @Captor
  ArgumentCaptor<SalesOrder> salesOrderArgumentCaptor;
  @InjectMocks
  private SqsReceiveService sqsReceiveService;

  private  ObjectMapper messageBodyMapper;

  @BeforeEach
  public void setUp(){
    ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
    messageBodyMapper = objectMapperConfig.getMapperForMessageBody();
    sqsReceiveService = new SqsReceiveService(runtimeService, salesOrderService, camundaHelper, mapper, messageBodyMapper,salesOrderRepository);
  }

  @Test
  public void testQueueListenerEcpShopOrdersWhenRecurringOrderReceived(){
    var senderId = "Ecp";
    String rawMessage =  readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = getSaleOrder(rawMessage);

    when(salesOrderRepository.countByCustomerEmail(salesOrder.getCustomerEmail())).thenReturn(2L);

    sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId);

    verify(salesOrderService, times(1)).save(any(SalesOrder.class));
    verify(salesOrderService, times(1)).save(salesOrderArgumentCaptor.capture());
    assertThat(salesOrderArgumentCaptor.getValue().isRecurringOrder()).isTrue();
    verify(camundaHelper, times(1)).createOrderProcess(any(SalesOrder.class),
                                                                              any(Messages.class));
  }

  @Test
  public void testQueueListenerEcpShopOrdersWhenNewOrderReceived(){
    var senderId = "Ecp";
    String rawMessage =  readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = getSaleOrder(rawMessage);

    when(salesOrderRepository.countByCustomerEmail(salesOrder.getCustomerEmail())).thenReturn(0L);

    sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId);

    verify(salesOrderService, times(1)).save(any(SalesOrder.class));
    verify(salesOrderService, times(1)).save(salesOrderArgumentCaptor.capture());
    assertThat(salesOrderArgumentCaptor.getValue().isRecurringOrder()).isFalse();
    verify(camundaHelper, times(1)).createOrderProcess(any(SalesOrder.class),
        any(Messages.class));
  }

  @SneakyThrows({URISyntaxException.class, IOException.class})
  private String readResource(String path) {
    return Files.toString(Paths.get(getClass().getClassLoader().getResource(path).toURI()).toFile(),
        Charsets.UTF_8);
  }

  @SneakyThrows(JsonProcessingException.class)
  private OrderJSON getOrderJson(String rawMessage){
    ObjectMapper mapper = new ObjectMapper();
    String message = configureMapperForMessageHeader(mapper).readValue(rawMessage, EcpOrder.class).getMessage();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper.readValue(message, OrderJSON.class);
  }

  private ObjectMapper configureMapperForMessageHeader(ObjectMapper mapper){
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
    return mapper;
  }

  private SalesOrder getSaleOrder(String rawMessage){
   return SalesOrder.builder()
                    .orderNumber("514000018")
                    .salesLocale(Locale.GERMANY.toString())
                    .customerEmail("test@kfzteile24.de")
                    .originalOrder(getOrderJson(rawMessage))
                    .build();
  }

}
