package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.mockito.Mockito.*;

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
    private SalesOrderRowService salesOrderRowService;
    @Mock
    private CamundaHelper camundaHelper;
    @InjectMocks
    private SqsReceiveService sqsReceiveService;

    @Test
    public void testQueueListenerEcpShopOrders() {
        var senderId = "Ecp";
        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);

        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);

        sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId);

        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService, never()).getOrderByOrderNumber(eq("524001240"));
    }

    @Test
    public void testQueueListenerCoreDuplicateShopOrders() {
        //This test case can be removed if no more duplicate items are expected from core publisher.
        var senderId = "Core";
        String rawMessage = readResource("examples/coreOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);

        when(salesOrderService.getOrderByOrderNumber("524001240")).thenReturn(Optional.of(salesOrder));

        sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId);

        verify(camundaHelper, never()).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).getOrderByOrderNumber(eq("524001240"));
    }

    @Test
    public void testQueueListenerCoreCancellation() throws JsonProcessingException {

        var senderId = "Ecp";
        String cancellationRawMessage = readResource("examples/coreCancellationOneRowMessage.json");

        sqsReceiveService.queueListenerCoreCancellation(cancellationRawMessage, senderId, 1);

        String body = objectMapper.readValue(cancellationRawMessage, SqsMessage.class).getBody();
        CoreCancellationMessage coreCancellationMessage = objectMapper.readValue(body, CoreCancellationMessage.class);
        verify(salesOrderRowService).cancelOrderRows(coreCancellationMessage);
  }

  @Test
  public void testQueueListenerSubsequentDeliveryReceived() {
    var senderId = "Delivery";
    var receiveCount = 1;
    String rawMessage =  readResource("examples/ecpOrderMessage.json");
    SalesOrder salesOrder = getSalesOrder(rawMessage);

    when(salesOrderService.createSalesOrderForSubsequentDelivery(any(), any())).thenReturn(salesOrder);

    String subsequentDeliveryNoteMessage = readResource("examples/subsequentDeliveryNote.json");
    sqsReceiveService.queueListenerSubsequentDeliveryReceived(subsequentDeliveryNoteMessage, senderId, receiveCount);

    verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }

}
