package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
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

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService.ORDER_FULFILLMENT_DELTICOM;
import static de.kfzteile24.salesOrderHub.services.splitter.decorator.OrderSplitService.ORDER_FULFILLMENT_K24;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void testSplitOrderIfNecessary() {
        var message = getObjectByResource("coreOrderMessage.json", Order.class);

        // Scenario 1: There are only dropshipment items (no split is needed, change fulfillment to delticom)
        message.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        when(orderUtil.containsOnlyDropShipmentItems(message))
                .thenReturn(true);
        var splittedOrder = orderSplitService.splitOrderIfNecessary(message);
        assertNull(splittedOrder);
        assertEquals(ORDER_FULFILLMENT_DELTICOM, message.getOrderHeader().getOrderFulfillment());

        // Scenario 2: There are some dropshipment items (split as usual)
        message.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        when(orderUtil.containsOnlyDropShipmentItems(message))
                .thenReturn(false);
        when(orderUtil.containsDropShipmentItems(message))
                .thenReturn(true);
        splittedOrder = orderSplitService.splitOrderIfNecessary(message);
        assertEquals(ORDER_FULFILLMENT_DELTICOM, splittedOrder.getOrderHeader().getOrderFulfillment());
        assertEquals(ORDER_FULFILLMENT_K24, message.getOrderHeader().getOrderFulfillment());


        // Scenario 3: There are no dropshipment items (no split is needed)
        message.getOrderHeader().setOrderFulfillment(ORDER_FULFILLMENT_K24);
        when(orderUtil.containsOnlyDropShipmentItems(message))
                .thenReturn(false);
        when(orderUtil.containsDropShipmentItems(message))
                .thenReturn(false);
        splittedOrder = orderSplitService.splitOrderIfNecessary(message);
        assertNull(splittedOrder);
        assertEquals(ORDER_FULFILLMENT_K24, message.getOrderHeader().getOrderFulfillment());
    }
}
