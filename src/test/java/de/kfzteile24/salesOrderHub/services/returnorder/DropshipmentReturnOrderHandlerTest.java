package de.kfzteile24.salesOrderHub.services.returnorder;

import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentReturnOrderHandlerTest {

    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private OrderUtil orderUtil;
    @InjectMocks
    private DropshipmentReturnOrderHandler dropshipmentReturnOrderHandler;

    @Test
    void getSalesOrderListWhenEmpty() {
        var orderGroupId = "123456789";
        when(salesOrderService.getOrderByOrderGroupId(orderGroupId)).thenReturn(new ArrayList<>());
        assertThat(dropshipmentReturnOrderHandler.getSalesOrderList(orderGroupId)).isEqualTo(new ArrayList<>());
    }
}