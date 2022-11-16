package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CleanupDropshipmentInvoiceRowTableDelegateTest {

    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private CleanupDropshipmentInvoiceRowTableDelegate cleanupDropshipmentInvoiceRowTableDelegate;

    @Test
    @SneakyThrows
    void theDelegateCancelsOrder() {

        doNothing().when(dropshipmentInvoiceRowService).deleteAll();

        cleanupDropshipmentInvoiceRowTableDelegate.execute(delegateExecution);

        verify(dropshipmentInvoiceRowService).deleteAll();
    }
}
