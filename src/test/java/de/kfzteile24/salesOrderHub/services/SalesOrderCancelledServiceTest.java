package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalesOrderCancelledServiceTest {

    @Mock
    private SalesOrderRowService salesOrderRowService;

    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private SalesOrderCancelledService salesOrderCancelledService;

    @Test
    void testHandleCoreSalesOrderCancelled() {
        var message = CoreSalesOrderCancelledMessage.builder().build();
        var messageWrapper = MessageWrapper.builder().build();
        var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.getLatestJson().getOrderRows().forEach(orderRows -> orderRows.setIsCancelled(true));
        var orderNumber = salesOrder.getOrderNumber();
        message.setOrderNumber(orderNumber);
        when(salesOrderRowService.cancelOrder(orderNumber)).thenReturn(salesOrder);

        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);

        verify(salesOrderRowService).cancelOrder(eq(orderNumber));
        verify(snsPublishService).publishOrderCancelled(eq(salesOrder.getLatestJson()));
    }
}