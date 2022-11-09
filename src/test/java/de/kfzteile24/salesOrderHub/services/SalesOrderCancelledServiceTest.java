package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
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
    private CamundaHelper camundaHelper;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private SalesOrderCancelledService salesOrderCancelledService;

    @Test
    void testHandleCoreSalesOrderCancelled() {
        var message = CoreSalesOrderCancelledMessage.builder().build();
        var messageWrapper = MessageWrapper.builder().build();
        var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var orderNumber = salesOrder.getOrderNumber();
        message.setOrderNumber(orderNumber);
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));

        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);

        verify(camundaHelper).correlateMessage(CORE_SALES_ORDER_CANCELLED, salesOrder,
                Variables.putValue(ORDER_NUMBER.getName(), orderNumber));
    }
}