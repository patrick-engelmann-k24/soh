package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.val;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateDropshipmentSubsequentOrderDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private DropshipmentOrderService dropshipmentOrderService;
    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Mock
    private DropshipmentOrderRowService dropshipmentOrderRowService;
    @Mock
    private MetricsHelper metricsHelper;
    @InjectMocks
    private CreateDropshipmentSubsequentOrderDelegate createDropshipmentSubsequentOrderDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void execute() {
        final var invoiceNumber = "456";
        final var orderNumber = "123";
        final var salesOrder =
                SalesOrder.builder().orderNumber(orderNumber).latestJson(Order.builder().build()).build();
        final var subsequentOrder =
                SalesOrder.builder().orderNumber(orderNumber + "-1").latestJson(Order.builder().build()).build();
        final var skuList = List.of("sku1", "sku2");
        val quantities = List.of(0, 0);
        final var invoiceData =
                InvoiceData.builder().invoiceNumber(invoiceNumber).orderNumber(orderNumber)
                        .orderRows(skuList)
                        .quantities(quantities).build();
        val skuQuantityMap = invoiceData.getSkuQuantityMap();
        when(delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(dropshipmentInvoiceRowService.getInvoiceData(invoiceNumber)).thenReturn(invoiceData);
        when(dropshipmentOrderRowService.getSkuListToBeCancelled(eq(orderNumber), eq(skuList))).thenReturn(skuList);
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        when(dropshipmentOrderService.createDropshipmentSubsequentSalesOrder(
                eq(salesOrder),
                eq(skuQuantityMap),
                eq(invoiceNumber),
                any())).thenReturn(subsequentOrder);
        createDropshipmentSubsequentOrderDelegate.execute(delegateExecution);
        verify(delegateExecution).setVariable(Variables.SUBSEQUENT_ORDER_NUMBER.getName(), subsequentOrder.getOrderNumber());
        verify(delegateExecution).setVariable(Variables.ORDER_NUMBER.getName(), orderNumber);
        verify(delegateExecution).setVariable(Variables.ORDER_ROWS.getName(), skuList);
    }
}