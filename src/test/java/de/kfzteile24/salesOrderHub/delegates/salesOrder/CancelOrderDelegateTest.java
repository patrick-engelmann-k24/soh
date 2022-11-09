package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelOrderDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private CancelOrderDelegate cancelOrderDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegateCancelsOrder() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        cancelOrderDelegate.execute(delegateExecution);
        verify(salesOrderService).cancelOrder(expectedOrderNumber);
        verify(delegateExecution).setVariable(Variables.IS_ORDER_CANCELLED.getName(), true);
    }

}
