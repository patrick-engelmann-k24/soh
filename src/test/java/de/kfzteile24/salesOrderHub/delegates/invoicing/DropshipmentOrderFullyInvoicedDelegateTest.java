package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.extension.mockito.process.ProcessInstanceFake;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_FULLY_INVOICED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DropshipmentOrderFullyInvoicedDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CamundaHelper camundaHelper;

    @Mock
    private MetricsHelper metricsHelper;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;


    @InjectMocks
    private DropshipmentOrderFullyInvoicedDelegate dropshipmentOrderFullyInvoicedDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testDropshipmentOrderFullyInvoicedDelegate() {
        final var expectedOrderNumber = "456";
        var salesOrder = SalesOrder.builder().build();
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(salesOrderService.getOrderByOrderNumber(expectedOrderNumber)).thenReturn(Optional.of(salesOrder));
        when(camundaHelper.correlateMessage(any(), anyString()).getProcessInstance())
                .thenReturn(ProcessInstanceFake.builder().build());
        dropshipmentOrderFullyInvoicedDelegate.execute(delegateExecution);
        verify(camundaHelper).correlateMessage(DROPSHIPMENT_ORDER_FULLY_INVOICED, expectedOrderNumber);
    }
}
