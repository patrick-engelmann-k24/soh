package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDropshipmentSubsequentInvoiceDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private InvoiceService invoiceService;
    @InjectMocks
    private CreateDropshipmentSubsequentInvoiceDelegate createDropshipmentSubsequentInvoiceDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void execute() {
        final var orderNumber = "123";
        final var salesOrder =
                SalesOrder.builder()
                        .orderNumber(orderNumber)
                        .latestJson(Order.builder().build())
                        .build();
        final var invoice =
                CoreSalesInvoiceCreatedMessage.builder()
                        .salesInvoice(CoreSalesInvoice.builder()
                                .salesInvoiceHeader(CoreSalesInvoiceHeader.builder()
                                        .invoiceNumber("456")
                                        .orderNumber(orderNumber)
                                        .orderGroupId(orderNumber)
                                        .build())
                                .build())
                        .build();
        when(delegateExecution.getVariable(Variables.SUBSEQUENT_ORDER_NUMBER.getName())).thenReturn(orderNumber);
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        when(invoiceService.generateInvoiceMessage(eq(salesOrder))).thenReturn(invoice);
        when(salesOrderService.save(any(), any())).thenReturn(salesOrder);
        createDropshipmentSubsequentInvoiceDelegate.execute(delegateExecution);
        salesOrder.setInvoiceEvent(invoice);
        verify(salesOrderService).save(eq(salesOrder), eq(DROPSHIPMENT_INVOICE_STORED));
    }
}