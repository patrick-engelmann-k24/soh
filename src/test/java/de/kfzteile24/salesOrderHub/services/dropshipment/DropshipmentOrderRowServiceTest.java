package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.dto.dropshipment.DropshipmentItemQuantity;
import de.kfzteile24.salesOrderHub.dto.dropshipment.DropshipmentOrderShipped;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_ORDER_SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderRowServiceTest {

    @InjectMocks
    private DropshipmentOrderRowService dropshipmentOrderRowService;
    @Mock
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Mock
    private CamundaHelper camundaHelper;
    @Mock
    private DropshipmentOrderRowRepository dropshipmentOrderRowRepository;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private DropshipmentHelper dropshipmentHelper;

    @Test
    void testIsItemsFullyShippedFalse() {
        var orderNumber = "123456789";
        List<DropshipmentOrderRow> dropshipmentOrderRows = new ArrayList<>();
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-1", 2, 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-2", 2, 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-3", 2, 1));
        when(dropshipmentOrderRowRepository.findByOrderNumber(anyString())).thenReturn(dropshipmentOrderRows);

        assertThat(dropshipmentOrderRowService.isItemsFullyShipped(orderNumber)).isFalse();
        verify(salesOrderService, never()).save(any(), eq(DROPSHIPMENT_ORDER_SHIPPED));
    }

    @Test
    void testIsItemsFullyShippedTrue() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment("delticom");
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));

        List<DropshipmentOrderRow> dropshipmentOrderRows = new ArrayList<>();
        dropshipmentOrderRows.add(createDropshipmentOrderRow(salesOrder.getOrderNumber(), "sku-1", 2, 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(salesOrder.getOrderNumber(), "sku-2", 2, 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(salesOrder.getOrderNumber(), "sku-3", 2, 2));
        when(dropshipmentOrderRowRepository.findByOrderNumber(anyString())).thenReturn(dropshipmentOrderRows);

        assertThat(dropshipmentOrderRowService.isItemsFullyShipped(salesOrder.getOrderNumber())).isTrue();
        salesOrder.setShipped(true);
        verify(salesOrderService).save(eq(salesOrder), eq(DROPSHIPMENT_ORDER_SHIPPED));
    }

    @Test
    void testIsItemsFullyShippedNoRows() {
        var orderNumber = "123456789";
        when(dropshipmentOrderRowRepository.findByOrderNumber(eq(orderNumber))).thenReturn(List.of());
        assertFalse(dropshipmentOrderRowService.isItemsFullyShipped(orderNumber));
    }


    private DropshipmentOrderRow createDropshipmentOrderRow(
            String orderNumber, String sku, Integer quantity, Integer quantityShipped) {
        return DropshipmentOrderRow.builder()
                .sku(sku)
                .orderNumber(orderNumber)
                .quantity(quantity)
                .quantityShipped(quantityShipped)
                .build();
    }

    @Test
    void testAddQuantityShipped() {
        var dropshipmentOrderRow = createDropshipmentOrderRow("123456789", "sku-1", 2, 2);
        when(dropshipmentOrderRowRepository.findBySkuAndOrderNumber(anyString(), anyString()))
                .thenReturn(Optional.of(dropshipmentOrderRow));

        dropshipmentOrderRowService.addQuantityShipped("sku-1", "123456789", 3);
        dropshipmentOrderRow.setQuantityShipped(5);
        verify(dropshipmentOrderRowRepository).save(argThat(entry -> {
                    assertThat(entry.getQuantityShipped()).isEqualTo(5);
                    return true;
                }
        ));
    }

    @Test
    void testShipItemsFullyShipped() {
        final var orderNumber = "123456789";
        SalesOrder salesOrder = SalesOrder.builder().build();
        DropshipmentOrderShipped dropshipmentOrderShipped = DropshipmentOrderShipped.builder()
                .orderNumber(orderNumber)
                .items(List.of(
                                DropshipmentItemQuantity.builder().sku("sku-1").quantity(3).build(),
                                DropshipmentItemQuantity.builder().sku("sku-2").quantity(1).build(),
                                DropshipmentItemQuantity.builder().sku("sku-3").quantity(2).build()
                        )
                )
                .build();
        final var dropshipmentOrderRow1 = createDropshipmentOrderRow(orderNumber, "sku-1", 5, 2);
        final var dropshipmentOrderRow2 = createDropshipmentOrderRow(orderNumber, "sku-2", 4, 3);
        final var dropshipmentOrderRow3 = createDropshipmentOrderRow(orderNumber, "sku-3", 3, 1);

        when(dropshipmentOrderRowRepository.findByOrderNumber(orderNumber)).thenReturn(
                List.of(dropshipmentOrderRow1, dropshipmentOrderRow2, dropshipmentOrderRow3));
        when(dropshipmentOrderRowRepository.save(eq(dropshipmentOrderRow1))).thenReturn(dropshipmentOrderRow1);
        when(dropshipmentOrderRowRepository.save(eq(dropshipmentOrderRow2))).thenReturn(dropshipmentOrderRow2);
        when(dropshipmentOrderRowRepository.save(eq(dropshipmentOrderRow3))).thenReturn(dropshipmentOrderRow3);
        when(dropshipmentOrderRowRepository.findBySkuAndOrderNumber(eq("sku-1"), eq(orderNumber)))
                .thenReturn(Optional.of(dropshipmentOrderRow1));
        when(dropshipmentOrderRowRepository.findBySkuAndOrderNumber(eq("sku-2"), eq(orderNumber)))
                .thenReturn(Optional.of(dropshipmentOrderRow2));
        when(dropshipmentOrderRowRepository.findBySkuAndOrderNumber(eq("sku-3"), eq(orderNumber)))
                .thenReturn(Optional.of(dropshipmentOrderRow3));
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.save(any(), eq(DROPSHIPMENT_ORDER_SHIPPED))).thenReturn(salesOrder);
        when(camundaHelper.correlateMessage(eq(DROPSHIPMENT_ORDER_FULLY_COMPLETED), eq(orderNumber))).thenReturn(null);

        dropshipmentOrderRowService.shipItems(dropshipmentOrderShipped);

        verify(dropshipmentOrderRowRepository).save(argThat(dor -> dor.getQuantityShipped() == 5));
        verify(dropshipmentOrderRowRepository).save(argThat(dor -> dor.getQuantityShipped() == 4));
        verify(dropshipmentOrderRowRepository).save(argThat(dor -> dor.getQuantityShipped() == 3));
        verify(dropshipmentInvoiceRowService).create(any(), eq(orderNumber), eq(3));
        verify(dropshipmentInvoiceRowService).create(any(), eq(orderNumber), eq(1));
        verify(dropshipmentInvoiceRowService).create(any(), eq(orderNumber), eq(2));
        verify(salesOrderService).save(argThat(SalesOrder::isShipped), eq(DROPSHIPMENT_ORDER_SHIPPED));
        verify(camundaHelper).correlateMessage(eq(DROPSHIPMENT_ORDER_FULLY_COMPLETED), eq(orderNumber));
    }

    @Test
    void testShipItems() {
        final var orderNumber = "123456789";
        SalesOrder salesOrder = SalesOrder.builder().build();
        DropshipmentOrderShipped dropshipmentOrderShipped = DropshipmentOrderShipped.builder()
                .orderNumber(orderNumber)
                .items(List.of(DropshipmentItemQuantity.builder().sku("sku-1").quantity(2).build()))
                .build();
        final var dropshipmentOrderRow = createDropshipmentOrderRow(orderNumber, "sku-1", 5, 2);

        when(dropshipmentOrderRowRepository.findByOrderNumber(orderNumber)).thenReturn(List.of(dropshipmentOrderRow));
        when(dropshipmentOrderRowRepository.save(eq(dropshipmentOrderRow))).thenReturn(dropshipmentOrderRow);
        when(dropshipmentOrderRowRepository.findBySkuAndOrderNumber(eq("sku-1"), eq(orderNumber)))
                .thenReturn(Optional.of(dropshipmentOrderRow));

        dropshipmentOrderRowService.shipItems(dropshipmentOrderShipped);

        verify(dropshipmentOrderRowRepository).save(argThat(dor -> dor.getQuantityShipped() == 4));
        verify(dropshipmentInvoiceRowService).create(any(), eq(orderNumber), eq(2));
        verify(salesOrderService, never()).save(argThat(SalesOrder::isShipped), eq(DROPSHIPMENT_ORDER_SHIPPED));
        verify(camundaHelper, never()).correlateMessage(eq(DROPSHIPMENT_ORDER_FULLY_COMPLETED), eq(orderNumber));
    }

    @Test
    void testGetSkuListToBeCancelled() {
        final var orderNumber = "123456789";
        List<DropshipmentOrderRow> dropshipmentOrderRows = new ArrayList<>();
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-1", 3, 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-2", 3, 3));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-3", 3, 3));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-4", 3, 4));
        when(dropshipmentOrderRowRepository.findByOrderNumber(anyString())).thenReturn(dropshipmentOrderRows);

        assertThat(dropshipmentOrderRowService.getSkuListToBeCancelled(orderNumber, List.of("sku-1", "sku-3", "sku-4"))).isEqualTo(List.of("sku-3", "sku-4"));
    }
}