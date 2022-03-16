package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
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
class OrderCancelledDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private OrderCancelledDelegate orderCancelledDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegateUpdatesTheSalesOrderPublishesAnOrderRowsCancelledEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        orderCancelledDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderCancelled(expectedOrderNumber);
    }

}