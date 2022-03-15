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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceSavedDelegateTest {

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private InvoiceSavedDelegate invoiceSavedDelegate;

    @Test
    @SneakyThrows
    void whenDropshipmentInvoiceUrl_thenPublishInvoiceCreatedEvent() {

        final var expectedOrderNumber = "123";
        final var expectedInvoiceUrl = "s3://production-k24-invoices/dropshipment/2021/06/04/514073754-710108946.pdf";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(Variables.INVOICE_URL.getName())).thenReturn(expectedInvoiceUrl);

        invoiceSavedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderInvoiceCreated(expectedOrderNumber);
    }

    @Test
    @SneakyThrows
    void whenNotDropshipmentInvoiceUrl_thenSkipPublishInvoiceCreatedEvent() {

        final var expectedOrderNumber = "123";
        final var expectedInvoiceUrl = "s3://production-k24-invoices/anyFolder/2021/06/04/514073754-710108946.pdf";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(Variables.INVOICE_URL.getName())).thenReturn(expectedInvoiceUrl);

        invoiceSavedDelegate.execute(delegateExecution);

        verify(snsPublishService, never()).publishOrderInvoiceCreated(anyString());
    }
}