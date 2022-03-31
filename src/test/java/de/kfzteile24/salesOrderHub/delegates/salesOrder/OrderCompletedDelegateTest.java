package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCompletedDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private OrderCompletedDelegate orderCompletedDelegate;

    @Mock
    private DelegateExecution delegateExecution;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegatePublishesAnOrderCompletedChangedEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        orderCompletedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderCompleted(expectedOrderNumber);
    }

}