package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartDropshipmentSubsequentOrderProcessDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private DropshipmentOrderService dropshipmentOrderService;
    @InjectMocks
    private StartDropshipmentSubsequentOrderProcessDelegate startDropshipmentSubsequentOrderProcessDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void execute() {
        final var orderNumber = "123";
        final var salesOrder =
                SalesOrder.builder()
                        .orderNumber(orderNumber)
                        .latestJson(Order.builder().build())
                        .build();
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(orderNumber);
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        startDropshipmentSubsequentOrderProcessDelegate.execute(delegateExecution);
        verify(dropshipmentOrderService).startDropshipmentSubsequentOrderProcess(eq(salesOrder));
    }
}