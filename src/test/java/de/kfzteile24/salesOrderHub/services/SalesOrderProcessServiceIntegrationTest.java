package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.ItemSplitService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@MockBean({
        ItemSplitService.class
})
public class SalesOrderProcessServiceIntegrationTest {

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderProcessService salesOrderProcessService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Test
    @SneakyThrows
    @DisplayName("Test Handle Shop Orders Received Splitted Orders")
    void testHandleShopOrdersReceivedSplittedOrders() {
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        order.getOrderHeader().setPlatform(Platform.BRAINCRAFT);
        order.getOrderRows().get(0).setGenart("10040");
        order.getOrderRows().get(1).setGenart("test");

        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .rawMessage(orderRawMessage)
                .message(order)
                .build();

        salesOrderProcessService.handleShopOrdersReceived(messageWrapper);

        SalesOrder regularOrder = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).get();
        assertNotNull(regularOrder);
        assertEquals(order.getOrderHeader().getOrderNumber(), regularOrder.getOrderNumber());
        assertEquals(order.getOrderHeader().getOrderNumber(), regularOrder.getLatestJson().getOrderHeader().getOrderNumber());
        assertEquals(K24.getName(), regularOrder.getLatestJson().getOrderHeader().getOrderFulfillment());
        assertEquals(1, regularOrder.getLatestJson().getOrderRows().size());
        assertEquals("test", regularOrder.getLatestJson().getOrderRows().get(0).getGenart());

        var optional = salesOrderService.getOrderByOrderNumber(regularOrder.getOrderNumber() + "-1");
        assertTrue(optional.isPresent());
        SalesOrder splittedOrder = optional.get();
        assertEquals(order.getOrderHeader().getOrderNumber() + "-1", splittedOrder.getOrderNumber());
        assertEquals(order.getOrderHeader().getOrderNumber() + "-1", splittedOrder.getLatestJson().getOrderHeader().getOrderNumber());
        assertEquals(DELTICOM.getName(), splittedOrder.getLatestJson().getOrderHeader().getOrderFulfillment());
        assertEquals(1, splittedOrder.getLatestJson().getOrderRows().size());
        assertEquals("10040", splittedOrder.getLatestJson().getOrderRows().get(0).getGenart());
    }

    @AfterEach
    public void cleanup() {
        try {
            salesOrderRepository.deleteAll();
        } catch (Exception e) {
            //ignore
        }
    }
}
