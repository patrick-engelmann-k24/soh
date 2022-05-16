package de.kfzteile24.salesOrderHub.services.splitter.decorator;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.DropShipmentConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class OrderSplitServiceTest {

    @Mock
    private ObjectMapperConfig objectMapperConfig;

    @Mock
    private DropShipmentConfig config;

    @Mock
    private OrderUtil orderUtil;

    @InjectMocks
    private OrderSplitService orderSplitService;

    @Test
    void calculateOrderListWithDSOrder() {

        final var order = getOrder(readResource("examples/splitterSalesOrderMessageWithTwoRows.json"));
        final var orderList = new ArrayList<Order>();
        orderList.add(order);

        doReturn(false).when(orderUtil).containsOnlyDropShipmentItems(any());
        doReturn(true).when(orderUtil).containsDropShipmentItems(any());
        doReturn(new ObjectMapper()).when(objectMapperConfig).objectMapper();
        orderSplitService.processOrderList(orderList);

        assertThat(orderList.size()).isGreaterThanOrEqualTo(2);
        final int originOrderIndex = orderList.indexOf(order);
        Order dsOrder;
        if (originOrderIndex == 0) {
            dsOrder = orderList.get(1);
        } else {
            dsOrder = orderList.get(0);
        }

        assertThat(dsOrder.getOrderHeader().getOrderNumber()).isNotEmpty();
        assertThat(dsOrder.getOrderHeader().getOrderNumber()).isNotEqualTo(order.getOrderHeader().getOrderNumber());
        assertThat(dsOrder.getOrderHeader().getOrderNumber()).isEqualTo(order.getOrderHeader().getOrderNumber().concat("-1"));
        assertThat(dsOrder.getOrderHeader().getOrderId().toString()).isNotEqualTo(order.getOrderHeader().getOrderId().toString());
    }

    @Test
    void calculateOrderListWithoutDSOrder() {
        final var order = getOrder(readResource("examples/ecpOrderMessage.json"));
        final var orderList = new ArrayList<Order>();
        orderList.add(order);
        orderSplitService.processOrderList(orderList);

        assertThat(orderList.size()).isGreaterThanOrEqualTo(1);
    }

}
