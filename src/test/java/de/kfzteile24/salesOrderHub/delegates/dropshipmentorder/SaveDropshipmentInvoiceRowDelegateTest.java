package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveDropshipmentInvoiceRowDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Mock
    private DropshipmentHelper dropshipmentHelper;

    @InjectMocks
    private SaveDropshipmentInvoiceRowDelegate saveDropshipmentInvoiceRowDelegate;

    @Test
    void testSaveDropshipmentInvoiceRowDelegate() throws Exception {
        final var expectedOrderNumber = "123";
        final var expectedSku = "456";
        final var expectedDropshipmentInvoiceRow = DropshipmentInvoiceRow.builder()
                        .sku(expectedSku).orderNumber(expectedOrderNumber).build();

        when(delegateExecution.getVariable(ORDER_ROW.getName())).thenReturn(expectedSku);
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(dropshipmentHelper.createDropshipmentInvoiceRow(eq(expectedSku), eq(expectedOrderNumber))).thenReturn(expectedDropshipmentInvoiceRow);

        saveDropshipmentInvoiceRowDelegate.execute(delegateExecution);

        verify(dropshipmentInvoiceRowService).save(expectedDropshipmentInvoiceRow);

    }
}