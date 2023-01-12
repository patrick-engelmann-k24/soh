package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishDropshipmentItemShipmentCompletedDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CamundaHelper camundaHelper;
    @Mock
    private SalesOrderService salesOrderService;
    @InjectMocks
    private PublishDropshipmentItemShipmentCompletedDelegate publishDropshipmentItemShipmentCompletedDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testDropshipmentOrderFullyShippedDelegate() {
        final var orderNumber = "123456789";
        final var salesOrder = SalesOrder.builder().build();
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(orderNumber);
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.ofNullable(salesOrder));
        publishDropshipmentItemShipmentCompletedDelegate.execute(delegateExecution);
        verify(camundaHelper).correlateMessage(DROPSHIPMENT_ORDER_FULLY_COMPLETED, salesOrder);
    }
}
