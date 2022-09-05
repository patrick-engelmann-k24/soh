package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.splitter.decorator.ItemSplitService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static de.kfzteile24.soh.order.dto.Platform.BRAINCRAFT;
import static de.kfzteile24.soh.order.dto.Platform.ECP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

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

    @SpyBean
    private SalesOrderService salesOrderService;

    @Test
    @SneakyThrows
    @DisplayName("Test Handle Shop Orders Received Splitted Orders")
    void testHandleShopOrdersReceivedSplittedOrders() {
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        order.getOrderHeader().setPlatform(BRAINCRAFT);
        order.getOrderRows().get(0).setGenart("10040");
        order.getOrderRows().get(1).setGenart("test");

        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .rawMessage(orderRawMessage)
                .message(order)
                .build();

        salesOrderProcessService.handleShopOrdersReceived(messageWrapper);

        SalesOrder regularOrder = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElseThrow();
        assertNotNull(regularOrder);
        assertEquals(order.getOrderHeader().getOrderNumber(), regularOrder.getOrderNumber());
        assertEquals(order.getOrderHeader().getOrderNumber(), regularOrder.getLatestJson().getOrderHeader().getOrderNumber());
        assertEquals(order.getOrderHeader().getOrderNumber(), regularOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertEquals(K24.getName(), regularOrder.getLatestJson().getOrderHeader().getOrderFulfillment());
        assertEquals(BRAINCRAFT, regularOrder.getLatestJson().getOrderHeader().getPlatform());
        assertEquals(1, regularOrder.getLatestJson().getOrderRows().size());
        assertEquals("test", regularOrder.getLatestJson().getOrderRows().get(0).getGenart());

        Order originalRegularOrder = (Order) regularOrder.getOriginalOrder();
        assertEquals(order.getOrderHeader().getOrderNumber(), originalRegularOrder.getOrderHeader().getOrderNumber());
        assertNull(originalRegularOrder.getOrderHeader().getOrderGroupId());
        assertEquals(K24.getName(), originalRegularOrder.getOrderHeader().getOrderFulfillment());
        assertEquals(ECP, originalRegularOrder.getOrderHeader().getPlatform());
        assertEquals(2, originalRegularOrder.getOrderRows().size());
        assertEquals("816", originalRegularOrder.getOrderRows().get(0).getGenart());
        assertEquals("816", originalRegularOrder.getOrderRows().get(1).getGenart());

        var optional = salesOrderService.getOrderByOrderNumber(regularOrder.getOrderNumber() + "-1");
        assertTrue(optional.isPresent());
        SalesOrder splittedOrder = optional.get();
        assertEquals(order.getOrderHeader().getOrderNumber() + "-1", splittedOrder.getOrderNumber());
        assertEquals(order.getOrderHeader().getOrderNumber() + "-1", splittedOrder.getLatestJson().getOrderHeader().getOrderNumber());
        assertEquals(order.getOrderHeader().getOrderNumber(), regularOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertEquals(DELTICOM.getName(), splittedOrder.getLatestJson().getOrderHeader().getOrderFulfillment());
        assertEquals(BRAINCRAFT, splittedOrder.getLatestJson().getOrderHeader().getPlatform());
        assertEquals(1, splittedOrder.getLatestJson().getOrderRows().size());
        assertEquals("10040", splittedOrder.getLatestJson().getOrderRows().get(0).getGenart());

        var originalSplittedOrder = (Order) splittedOrder.getOriginalOrder();
        assertEquals(order.getOrderHeader().getOrderNumber(), originalSplittedOrder.getOrderHeader().getOrderNumber());
        assertNull(originalSplittedOrder.getOrderHeader().getOrderGroupId());
        assertEquals(K24.getName(), originalSplittedOrder.getOrderHeader().getOrderFulfillment());
        assertEquals(ECP, originalSplittedOrder.getOrderHeader().getPlatform());
        assertEquals(2, originalSplittedOrder.getOrderRows().size());
        assertEquals("816", originalSplittedOrder.getOrderRows().get(0).getGenart());
        assertEquals("816", originalSplittedOrder.getOrderRows().get(1).getGenart());

        verify(salesOrderService).enrichInitialOrder(order);
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
