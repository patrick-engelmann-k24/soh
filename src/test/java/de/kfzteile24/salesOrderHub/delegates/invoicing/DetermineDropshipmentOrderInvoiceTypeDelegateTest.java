package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
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
class DetermineDropshipmentOrderInvoiceTypeDelegateTest {

    private final String invoiceNumber = "2022-1000000000001";
    private final InvoiceData invoiceData = new InvoiceData(invoiceNumber, "orderNumber", List.of("sku1.1", "sku1.2"), List.of(0, 0));

    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @InjectMocks
    private DetermineDropshipmentOrderInvoiceTypeDelegate determineInvoiceTypeDelegate;
    @Mock
    private SalesOrderService salesOrderService;

    @Test
    @SneakyThrows
    void testDropshipmentOrderFullInvoiceType() {

        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(eq(invoiceNumber))).thenReturn(invoiceData);
        when(salesOrderService.isFullyInvoiced(eq(invoiceData))).thenReturn(true);

        determineInvoiceTypeDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(eq(Variables.IS_PARTIAL_INVOICE.getName()), eq(false));
    }

    @Test
    @SneakyThrows
    void testDropshipmentOrderPartialInvoiceType() {

        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(eq(invoiceNumber))).thenReturn(invoiceData);
        when(salesOrderService.isFullyInvoiced(invoiceData)).thenReturn(false);

        determineInvoiceTypeDelegate.execute(delegateExecution);

        verify(delegateExecution).setVariable(eq(Variables.IS_PARTIAL_INVOICE.getName()), eq(true));
    }

    @Test
    @SneakyThrows
    void testIfDropshipmentInvoiceRowServiceThrowsException() {

        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(eq(invoiceNumber))).thenThrow(new NotFoundException(invoiceNumber));

        assertThatThrownBy(() -> determineInvoiceTypeDelegate.execute(delegateExecution))
                .isInstanceOf(NotFoundException.class);

        verify(salesOrderService, never()).isFullyInvoiced(any());
        verify(delegateExecution, never()).setVariable(eq(Variables.IS_PARTIAL_INVOICE.getName()), any());
    }
}