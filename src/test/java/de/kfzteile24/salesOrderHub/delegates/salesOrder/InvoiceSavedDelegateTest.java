package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceSavedDelegateTest {

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private DelegateExecution delegateExecution;

    @InjectMocks
    private InvoiceSavedDelegate invoiceSavedDelegate;

    private final String expectedInvoiceUrl = "s3://production-k24-invoices/anyFolder/2021/06/04/xxxxxxxxx-xxxxxxxxx.pdf";

    @Test
    @SneakyThrows
    void whenDropshipmentInvoiceUrlThenPublishInvoiceCreatedEvent() {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var originalOrder = (Order) salesOrder.getOriginalOrder();
        originalOrder.getOrderHeader().setOrderFulfillment("delticom");
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(salesOrder.getOrderNumber());
        when(delegateExecution.getVariable(Variables.INVOICE_URL.getName())).thenReturn(expectedInvoiceUrl);
        when(delegateExecution.getVariable(Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName())).thenReturn(false);
        when(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())).thenReturn(Optional.of(salesOrder));

        invoiceSavedDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderInvoiceCreated(salesOrder.getOrderNumber(), expectedInvoiceUrl);
    }

    @Test
    @SneakyThrows
    void whenDuplicateDropshipmentInvoiceUrlThenNeverPublishInvoiceCreatedEvent() {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var originalOrder = (Order) salesOrder.getOriginalOrder();
        originalOrder.getOrderHeader().setOrderFulfillment("delticom");
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(salesOrder.getOrderNumber());
        when(delegateExecution.getVariable(Variables.INVOICE_URL.getName())).thenReturn(expectedInvoiceUrl);
        when(delegateExecution.getVariable(Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName())).thenReturn(true);
        when(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())).thenReturn(Optional.of(salesOrder));

        invoiceSavedDelegate.execute(delegateExecution);

        verify(snsPublishService, never()).publishOrderInvoiceCreated(salesOrder.getOrderNumber(), expectedInvoiceUrl);
    }

    @Test
    @SneakyThrows
    void whenNotDropshipmentInvoiceUrlThenSkipPublishInvoiceCreatedEvent() {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var originalOrder = (Order) salesOrder.getOriginalOrder();
        originalOrder.getOrderHeader().setOrderFulfillment("K24");
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(salesOrder.getOrderNumber());
        when(delegateExecution.getVariable(Variables.INVOICE_URL.getName())).thenReturn(expectedInvoiceUrl);
        when(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())).thenReturn(Optional.of(salesOrder));

        invoiceSavedDelegate.execute(delegateExecution);

        verify(snsPublishService, never()).publishOrderInvoiceCreated(anyString(), anyString());
    }
}