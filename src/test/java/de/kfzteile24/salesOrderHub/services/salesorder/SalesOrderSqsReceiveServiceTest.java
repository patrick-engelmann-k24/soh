package de.kfzteile24.salesOrderHub.services.salesorder;

import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
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

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
        var message = CoreSalesOrderCancelledMessage.builder().build();
        var messageWrapper = MessageWrapper.builder().build();

        salesOrderSqsReceiveService.queueListenerCoreSalesOrderCancelled(message, messageWrapper);

        verify(salesOrderCancelledService).handleCoreSalesOrderCancelled(message, messageWrapper);

        verifyNoMoreInteractions(salesOrderCancelledService);
    }
}
