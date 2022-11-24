package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
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
public class GenerateInvoicePdfDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @InjectMocks
    private GenerateInvoicePdfDelegate generateInvoicePdfDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testGenerateInvoicePdfDelegate() {
        final var expectedInvoiceNumber = "123";
        final var expectedOrderNumber = "456";
        final var expectedOrder = SalesOrder.builder().orderNumber(expectedOrderNumber).latestJson(Order.builder().build()).build();
        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(expectedInvoiceNumber);
        when(dropshipmentInvoiceRowService.getOrderNumberByInvoiceNumber(eq(expectedInvoiceNumber))).thenReturn(expectedOrderNumber);
        when(salesOrderService.getOrderByOrderNumber(expectedOrder.getOrderNumber())).thenReturn(Optional.of(expectedOrder));
        generateInvoicePdfDelegate.execute(delegateExecution);
        verify(snsPublishService).publishInvoicePdfGenerationTriggeredEvent(expectedOrder.getLatestJson());
    }
}
