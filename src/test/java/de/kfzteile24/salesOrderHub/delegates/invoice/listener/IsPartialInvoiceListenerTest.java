package de.kfzteile24.salesOrderHub.delegates.invoice.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.invoicing.listener.IsPartialInvoiceListener;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class IsPartialInvoiceListenerTest {

    private final String invoiceNumber = "2022-1000000000001";
    private final InvoiceData invoiceData = new InvoiceData(invoiceNumber, "orderNumber", List.of("sku1.1", "sku1.2"));

    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @InjectMocks
    private IsPartialInvoiceListener isPartialInvoiceListener;
    @Mock
    private SalesOrderService salesOrderService;

    @Test
    @SneakyThrows
    void testIsPartialInvoiceListenerFalseResult() {

        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(eq(invoiceNumber))).thenReturn(invoiceData);
        when(salesOrderService.isFullyMatched(eq(invoiceData.getOrderRows()), eq(invoiceData.getOrderNumber()))).thenReturn(true);

        isPartialInvoiceListener.notify(delegateExecution);

        verify(delegateExecution).setVariable(eq(Variables.IS_PARTIAL_INVOICE.getName()), eq(false));
    }

    @Test
    @SneakyThrows
    void testIsPartialInvoiceListenerTrueResult() {

        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(eq(invoiceNumber))).thenReturn(invoiceData);
        when(salesOrderService.isFullyMatched(eq(invoiceData.getOrderRows()), eq(invoiceData.getOrderNumber()))).thenReturn(false);

        isPartialInvoiceListener.notify(delegateExecution);

        verify(delegateExecution).setVariable(eq(Variables.IS_PARTIAL_INVOICE.getName()), eq(true));
    }

    @Test
    @SneakyThrows
    void testIsPartialInvoiceListenerIfDropshipmentInvoiceRowServiceThrowsException() {

        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(eq(invoiceNumber))).thenThrow(new NotFoundException(invoiceNumber));

        assertThatThrownBy(() -> isPartialInvoiceListener.notify(delegateExecution))
                .isInstanceOf(NotFoundException.class);

        verify(salesOrderService, never()).isFullyMatched(any(), any());
        verify(delegateExecution, never()).setVariable(eq(Variables.IS_PARTIAL_INVOICE.getName()), any());
    }
}