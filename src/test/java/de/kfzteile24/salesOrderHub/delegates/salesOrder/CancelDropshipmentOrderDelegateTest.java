package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CancelDropshipmentOrderDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private CamundaHelper camundaHelper;
    @Mock
    private MetricsHelper metricsHelper;
    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private CancelDropshipmentOrderDelegate cancelDropshipmentOrderDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegateCancelsOrder() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(salesOrderService.getOrderByOrderNumber(expectedOrderNumber)).thenReturn(Optional.of(SalesOrder.builder().build()));
        cancelDropshipmentOrderDelegate.execute(delegateExecution);
        verify(camundaHelper).correlateDropshipmentOrderCancelledMessage(expectedOrderNumber);
    }
}
