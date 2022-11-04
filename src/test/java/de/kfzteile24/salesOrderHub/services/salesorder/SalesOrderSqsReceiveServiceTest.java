package de.kfzteile24.salesOrderHub.services.salesorder;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderCancelledService;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.PAYPAL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
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

    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    @Mock
    private SalesOrderCancelledService salesOrderCancelledService;
    @InjectMocks
    @Spy
    private SalesOrderSqsReceiveService salesOrderSqsReceiveService;

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedWithPaypalPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, PAYPAL, NEW);
        salesOrder.setOrderNumber("500000996");

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(anyString());
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));

        var message = getObjectByResource("coreDataReaderEvent.json",  CoreDataReaderEvent.class);
        salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message);

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

        var message = getObjectByResource("coreDataReaderEvent.json",  CoreDataReaderEvent.class);
        salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedSalesOrderNotFoundException() {
        var message = getObjectByResource("coreDataReaderEvent.json",  CoreDataReaderEvent.class);

        var exception = new SalesOrderNotFoundException("500000996");

        when(salesOrderService.getOrderByOrderNumber(any())).thenThrow(exception);

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message))
                .isExactlyInstanceOf(SalesOrderNotFoundException.class)
                .hasMessage(exception.getMessage());

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedIllegalArgumentException() {

        var message = getObjectByResource("coreDataReaderEvent.json",  CoreDataReaderEvent.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerOrderPaymentSecuredReceivedRuntimeExceptionException() {
        var message = getObjectByResource("coreDataReaderEvent.json",  CoreDataReaderEvent.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(false);
        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        assertThatThrownBy(() -> salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message))
                .isExactlyInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceived() {
        var message = getObjectByResource("d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json",  OrderPaymentSecuredMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        salesOrderSqsReceiveService.queueListenerD365OrderPaymentSecured(message, messageWrapper);

        verify(salesOrderPaymentSecuredService).handleD365OrderPaymentSecured(message, messageWrapper);

        verifyNoMoreInteractions(salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerCoreSalesOrderCancelled() {
        var message = getObjectByResource("coreSalesOrderCancelledMessage.json",  CoreSalesOrderCancelledMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        salesOrderSqsReceiveService.queueListenerCoreSalesOrderCancelled(message, messageWrapper);

        verify(salesOrderCancelledService).handleCoreSalesOrderCancelled(message, messageWrapper);

        verifyNoMoreInteractions(salesOrderCancelledService);
    }
}
