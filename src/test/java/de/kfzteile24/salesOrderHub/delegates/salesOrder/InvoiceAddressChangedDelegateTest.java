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
class InvoiceAddressChangedDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private InvoiceAddressChangedDelegate invoiceAddressChangedDelegate;

    @Mock
    DelegateExecution delegateExecution;

    @Test
    @SneakyThrows(Exception.class)
    public void theDelegatePublishesAnInvoiceAddressChangedEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        invoiceAddressChangedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishInvoiceAddressChanged(expectedOrderNumber);
    }

}