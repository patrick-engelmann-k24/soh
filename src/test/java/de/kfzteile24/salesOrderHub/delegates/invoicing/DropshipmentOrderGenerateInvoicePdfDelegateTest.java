package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DropshipmentOrderGenerateInvoicePdfDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private DropshipmentOrderGenerateInvoicePdfDelegate dropshipmentOrderGenerateInvoicePdfDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testGenerateInvoicePdfDelegate() {
        final var expectedOrderNumber = "456";
        final var expectedOrder = SalesOrder.builder().orderNumber(expectedOrderNumber).latestJson(Order.builder().build()).build();
        expectedOrder.setId(UUID.randomUUID());
        when(delegateExecution.getVariable(Variables.SALES_ORDER_ID.getName())).thenReturn(expectedOrder.getId());
        when(salesOrderService.getOrderById(expectedOrder.getId())).thenReturn(Optional.of(expectedOrder));
        dropshipmentOrderGenerateInvoicePdfDelegate.execute(delegateExecution);
        verify(snsPublishService).publishInvoicePdfGenerationTriggeredEvent(expectedOrder.getLatestJson());
    }
}
