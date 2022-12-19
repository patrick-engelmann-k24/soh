package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubsequentSalesOrderCreationHelperTest {

    @Mock
    private OrderUtil orderUtil;
    @InjectMocks
    @Spy
    private SubsequentSalesOrderCreationHelper subsequentSalesOrderCreationHelper;

    @Test
    void testCreateOrderHeaderWithNewOrderNumber() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class));
        String orderGroupId = "500";
        String newOrderNumber = "511";
        String invoiceNumber = "2023";
        when(orderUtil.copyOrderJson(salesOrder.getLatestJson())).thenReturn(salesOrder.getLatestJson());

        salesOrder.setOrderGroupId(orderGroupId);
        var result = subsequentSalesOrderCreationHelper.createOrderHeader(
                salesOrder, newOrderNumber, invoiceNumber);

        assertThat(result.getPlatform()).isEqualTo(Platform.SOH);
        assertThat(result.getOrderNumber()).isNotEqualTo(salesOrder.getOrderNumber());
        assertThat(result.getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(result.getOrderGroupId()).isEqualTo(orderGroupId);
        assertThat(result.getDocumentRefNumber()).isEqualTo(invoiceNumber);
    }
}