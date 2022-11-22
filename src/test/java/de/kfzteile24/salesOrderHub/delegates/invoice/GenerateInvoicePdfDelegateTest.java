package de.kfzteile24.salesOrderHub.delegates.invoice;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.DropshipmentOrderFullyInvoicedDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GenerateInvoicePdfDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private GenerateInvoicePdfDelegate generateInvoicePdfDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegateCancelsOrder() {
        final var expectedOrder = SalesOrder.builder().orderNumber("1234").latestJson(Order.builder().build()).build();
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrder.getOrderNumber());
        when(salesOrderService.getOrderByOrderNumber(expectedOrder.getOrderNumber())).thenReturn(Optional.of(expectedOrder));
        generateInvoicePdfDelegate.execute(delegateExecution);
        verify(snsPublishService).publishInvoicePdfGenerationTriggeredEvent(expectedOrder.getLatestJson());
    }
}
