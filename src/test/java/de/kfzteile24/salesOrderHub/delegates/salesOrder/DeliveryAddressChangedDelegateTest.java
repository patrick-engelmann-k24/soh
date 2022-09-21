package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.row.DeliveryAddressChangedDelegate;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryAddressChangedDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private DeliveryAddressChangedDelegate deliveryAddressChangedDelegate;

    @Mock
    private DelegateExecution delegateExecution;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegatePublishesAnDeliveryAddressChangedEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        deliveryAddressChangedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishDeliveryAddressChanged(expectedOrderNumber);
    }

}