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
class OrderCreatedDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private OrderCreatedDelegate orderCreatedDelegate;

    @Mock
    private DelegateExecution delegateExecution;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegatePublishesAnOrderCreatedEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        orderCreatedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderCreated(expectedOrderNumber);
    }

}