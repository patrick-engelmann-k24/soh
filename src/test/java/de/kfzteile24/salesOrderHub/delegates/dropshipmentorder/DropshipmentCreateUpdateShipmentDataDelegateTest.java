package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ITEMS_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.QUANTITY_SHIPPED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentCreateUpdateShipmentDataDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private DropshipmentOrderRowService dropshipmentOrderRowService;

    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @InjectMocks
    private DropshipmentCreateUpdateShipmentDataDelegate dropshipmentCreateUpdateShipmentDataDelegate;

    @Test
    void testExecuteWhenQuantityShippedIsLowerThanTotalQuantity() throws Exception {
        final var orderNumber = "123456789";
        final var sku = "sku-1";
        final var quantityShipped = 2;
        final var dropshipmentOrderRow = DropshipmentOrderRow.builder()
                .orderNumber(orderNumber)
                .sku(sku)
                .quantity(3)
                .quantityShipped(2)
                .build();
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(orderNumber);
        when(delegateExecution.getVariable(ORDER_ROW.getName())).thenReturn(sku);
        when(delegateExecution.getVariable(QUANTITY_SHIPPED.getName())).thenReturn(quantityShipped);
        when(dropshipmentOrderRowService.addQuantityShipped(eq(sku), eq(orderNumber), eq(quantityShipped))).thenReturn(dropshipmentOrderRow);
        when(dropshipmentOrderRowService.isItemsFullyShipped(eq(orderNumber))).thenReturn(false);

        dropshipmentCreateUpdateShipmentDataDelegate.execute(delegateExecution);
        verify(dropshipmentInvoiceRowService).create(eq(sku), eq(orderNumber), eq(quantityShipped));
        verify(delegateExecution).setVariable(ITEMS_FULLY_SHIPPED.getName(), false);
    }

    @Test
    void testExecuteWhenQuantityShippedIsLargerThanTotalQuantity() throws Exception {
        final var orderNumber = "123456789";
        final var sku = "sku-1";
        final var quantityShipped = 4;
        final var dropshipmentOrderRow = DropshipmentOrderRow.builder()
                .orderNumber(orderNumber)
                .sku(sku)
                .quantity(6)
                .quantityShipped(7)
                .build();
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(orderNumber);
        when(delegateExecution.getVariable(ORDER_ROW.getName())).thenReturn(sku);
        when(delegateExecution.getVariable(QUANTITY_SHIPPED.getName())).thenReturn(quantityShipped);
        when(dropshipmentOrderRowService.addQuantityShipped(eq(sku), eq(orderNumber), eq(quantityShipped))).thenReturn(dropshipmentOrderRow);
        when(dropshipmentOrderRowService.isItemsFullyShipped(eq(orderNumber))).thenReturn(true);

        dropshipmentCreateUpdateShipmentDataDelegate.execute(delegateExecution);
        verify(dropshipmentInvoiceRowService).create(eq(sku), eq(orderNumber), eq(3));
        verify(delegateExecution).setVariable(ITEMS_FULLY_SHIPPED.getName(), true);
    }
}