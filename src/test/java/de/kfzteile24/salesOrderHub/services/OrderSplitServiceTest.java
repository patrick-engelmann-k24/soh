package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService.ORDER_FULFILLMENT_DELTICOM;
import static de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService.ORDER_FULFILLMENT_K24;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderSplitServiceTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Mock
    private OrderUtil orderUtil;

    @InjectMocks
    private OrderSplitService orderSplitService;

    @Test
    @SneakyThrows
    public void testSplitOrderIfNecessary() {
        String rawMessage = readResource("examples/coreOrderMessage.json");
        var sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        var order = objectMapper.readValue(sqsMessage.getBody(), Order.class);

        // Scenario 1: There are only dropshipment items (no split is needed, change fulfillment to delticom)
        order.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        when(orderUtil.containsOnlyDropShipmentItems(eq(order)))
                .thenReturn(true);
        var splittedOrder = orderSplitService.splitOrderIfNecessary(order);
        assertNull(splittedOrder);
        assertEquals(ORDER_FULFILLMENT_DELTICOM, order.getOrderHeader().getOrderFulfillment());

        // Scenario 2: There are some dropshipment items (split as usual)
        order.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        when(orderUtil.containsOnlyDropShipmentItems(eq(order)))
                .thenReturn(false);
        when(orderUtil.containsDropShipmentItems(eq(order)))
                .thenReturn(true);
        splittedOrder = orderSplitService.splitOrderIfNecessary(order);
        assertEquals(ORDER_FULFILLMENT_DELTICOM, splittedOrder.getOrderHeader().getOrderFulfillment());
        assertEquals(ORDER_FULFILLMENT_K24, order.getOrderHeader().getOrderFulfillment());


        // Scenario 3: There are no dropshipment items (no split is needed)
        order.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        when(orderUtil.containsOnlyDropShipmentItems(eq(order)))
                .thenReturn(false);
        when(orderUtil.containsDropShipmentItems(eq(order)))
                .thenReturn(false);
        splittedOrder = orderSplitService.splitOrderIfNecessary(order);
        assertNull(splittedOrder);
        assertEquals(ORDER_FULFILLMENT_K24, order.getOrderHeader().getOrderFulfillment());
    }
}
