package de.kfzteile24.salesOrderHub.services.salesorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.PAYPAL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class SalesOrderSqsReceiveServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private MessageWrapperUtil messageWrapperUtil;
    @Mock
    private SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    @Mock
    private DropshipmentOrderService dropshipmentOrderService;
    @InjectMocks
    @Spy
    private SalesOrderSqsReceiveService salesOrderSqsReceiveService;
    @Spy
    private MessageErrorHandler messageErrorHandler;

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedWithPaypalPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, PAYPAL, NEW);
        salesOrder.setOrderNumber("500000996");

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(anyString());
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);
        salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);
        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured("500000996");

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerOrderPaymentSecuredReceivedWithCreditcardPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber("500000996");

        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(true);

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);
        salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedSalesOrderNotFoundException() {
        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);

        var exception = new SalesOrderNotFoundException("500000996");

        when(salesOrderService.getOrderByOrderNumber(any())).thenThrow(exception);

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(SalesOrderNotFoundException.class)
                .hasMessage(exception.getMessage());

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedIllegalArgumentException() {

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerOrderPaymentSecuredReceivedRuntimeExceptionException() {

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(false);
        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceived() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");
        mockMessageWrapper(rawMessage,  OrderPaymentSecuredMessage.class);

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        salesOrderSqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567787")),
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567858"))
        );

        verifyNoMoreInteractions(salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerD365OrderPaymentSecuredDropshipmentOrderReceived() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");
        mockMessageWrapper(rawMessage,  OrderPaymentSecuredMessage.class);


        when(dropshipmentOrderService.isDropShipmentOrder("4567787")).thenReturn(true);
        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        salesOrderSqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured( "4567858");

        verifyNoMoreInteractions(salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceivedThrownException() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");
        mockMessageWrapper(rawMessage,  OrderPaymentSecuredMessage.class);

        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(
                "4567787", "4567858");

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT)).isExactlyInstanceOf(RuntimeException.class);

    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }

    @SneakyThrows
    private <T> void mockMessageWrapper(String rawMessage, Class<T> clazz) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        T message = objectMapper.readValue(body, clazz);
        var messageWrapper = MessageWrapper.<T>builder()
                .message(message)
                .rawMessage(rawMessage)
                .build();
        when(messageWrapperUtil.create(eq(rawMessage), eq(clazz)))
                .thenReturn(messageWrapper);
    }


}
