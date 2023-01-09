package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_ORDER_SHIPPED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderRowServiceTest {

    @InjectMocks
    private DropshipmentOrderRowService dropshipmentOrderRowService;
    @Mock
    private DropshipmentOrderRowRepository dropshipmentOrderRowRepository;
    @Mock
    private DropshipmentHelper dropshipmentHelper;
    @Mock
    private SalesOrderService salesOrderService;

    @Test
    @MethodSource("isItemsFullyShipped is false")
    void testIsItemsFullyShippedFalse() {
        var orderNumber = "123456789";
        List<DropshipmentOrderRow> dropshipmentOrderRows = new ArrayList<>();
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-1", 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-2", 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(orderNumber, "sku-3", 1));
        when(dropshipmentOrderRowRepository.findByOrderNumber(anyString())).thenReturn(dropshipmentOrderRows);

        assertThat(dropshipmentOrderRowService.isItemsFullyShipped(orderNumber)).isFalse();
        verify(salesOrderService, never()).save(any(), eq(DROPSHIPMENT_ORDER_SHIPPED));
    }

    @Test
    @MethodSource("isItemsFullyShipped is true")
    void testIsItemsFullyShippedTrue() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment("delticom");
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));

        List<DropshipmentOrderRow> dropshipmentOrderRows = new ArrayList<>();
        dropshipmentOrderRows.add(createDropshipmentOrderRow(salesOrder.getOrderNumber(), "sku-1", 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(salesOrder.getOrderNumber(), "sku-2", 2));
        dropshipmentOrderRows.add(createDropshipmentOrderRow(salesOrder.getOrderNumber(), "sku-3", 2));
        when(dropshipmentOrderRowRepository.findByOrderNumber(anyString())).thenReturn(dropshipmentOrderRows);

        assertThat(dropshipmentOrderRowService.isItemsFullyShipped(salesOrder.getOrderNumber())).isTrue();
        salesOrder.setShipped(true);
        verify(salesOrderService).save(eq(salesOrder), eq(DROPSHIPMENT_ORDER_SHIPPED));
    }

    private DropshipmentOrderRow createDropshipmentOrderRow(String orderNumber, String sku, Integer quantity) {
        return DropshipmentOrderRow.builder()
                .sku(sku)
                .orderNumber(orderNumber)
                .quantity(2)
                .quantityShipped(quantity)
                .build();
    }

    @Test
    @MethodSource("addQuantityShipped")
    void testAddQuantityShipped() {
        var dropshipmentOrderRow = createDropshipmentOrderRow("123456789", "sku-1", 2);
        when(dropshipmentOrderRowRepository.findBySkuAndOrderNumber(anyString(), anyString())).thenReturn(Optional.of(dropshipmentOrderRow));

        dropshipmentOrderRowService.addQuantityShipped("sku-1", "123456789", 3);
        verify(dropshipmentOrderRowRepository).save(eq(dropshipmentOrderRow));
    }
}