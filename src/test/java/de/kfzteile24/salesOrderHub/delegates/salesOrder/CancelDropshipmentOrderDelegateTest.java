package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
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
public class CancelDropshipmentOrderDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private CamundaHelper camundaHelper;

    @InjectMocks
    private CancelDropshipmentOrderDelegate cancelDropshipmentOrderDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void theDelegateCancelsOrder() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        cancelDropshipmentOrderDelegate.execute(delegateExecution);
        verify(camundaHelper).correlateDropshipmentOrderCancelledMessage(expectedOrderNumber);
    }
}
