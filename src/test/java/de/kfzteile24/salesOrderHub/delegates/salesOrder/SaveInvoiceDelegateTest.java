package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.export.AmazonS3Service;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource.SOH;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveInvoiceDelegateTest {

    private final static String ORDER_NUMBER = "123456789";
    private final static String INVOICE_NUMBER = "987654321";
    private final static String INVOICE_URL = "s3://production-k24-invoices/www-k24-de/2021/06/04/123456789-987654321.pdf";

    @InjectMocks
    private SaveInvoiceDelegate saveInvoiceDelegate;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private DropshipmentOrderService dropshipmentOrderService;

    @Mock
    private AmazonS3Service amazonS3Service;

    private SalesOrderInvoice invoice;
    private SalesOrder salesOrder;

    @BeforeEach
    private void setUp() {

        invoice = SalesOrderInvoice.builder()
                .orderNumber(ORDER_NUMBER)
                .invoiceNumber(INVOICE_NUMBER)
                .url(INVOICE_URL)
                .source(SOH)
                .build();

        salesOrder = SalesOrder.builder()
                .orderNumber(ORDER_NUMBER)
                .salesOrderInvoiceList(Set.of(invoice))
                .build();

        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(ORDER_NUMBER);
        when(delegateExecution.getVariable(Variables.INVOICE_URL.getName())).thenReturn(INVOICE_URL);
        when(salesOrderService.getOrderByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(salesOrder));
    }

    @Test
    @SneakyThrows
    void testSaveInvoiceDelegateNewInvoice() {

        when(invoiceService.getSalesOrderInvoiceByInvoiceNumber(INVOICE_NUMBER)).thenReturn(Optional.empty());
        when(dropshipmentOrderService.isDropShipmentOrder(ORDER_NUMBER)).thenReturn(true);

        saveInvoiceDelegate.execute(delegateExecution);

        verify(invoiceService).getSalesOrderInvoiceByInvoiceNumber(INVOICE_NUMBER);
        verify(invoiceService).saveInvoice(invoice);
        verify(invoiceService).addSalesOrderToInvoice(salesOrder, invoice);
        verify(delegateExecution).setVariable(Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName(), false);
    }

    @Test
    @SneakyThrows
    void testSaveInvoiceDelegateDuplicateInvoice() {

        final var oldInvoiceUrl = "s3://production-k24-invoices/www-k24-de/2020/10/14/123456789-987654321.pdf";
        invoice.setUrl(oldInvoiceUrl);

        when(invoiceService.getSalesOrderInvoiceByInvoiceNumber(INVOICE_NUMBER)).thenReturn(Optional.of(invoice));

        saveInvoiceDelegate.execute(delegateExecution);

        verify(invoiceService).getSalesOrderInvoiceByInvoiceNumber(INVOICE_NUMBER);
        verify(amazonS3Service).deleteFile(oldInvoiceUrl);
        verify(invoiceService).saveInvoice(invoice);
        verify(invoiceService).addSalesOrderToInvoice(salesOrder, invoice);
        verify(delegateExecution).setVariable(Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName(), true);
    }

}
