package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static de.kfzteile24.soh.order.dto.Platform.BRAINCRAFT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderProcessServiceTest {

    @Spy
    private ObjectMapperConfig objectMapperConfig;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private CamundaHelper camundaHelper;
    @Mock
    private SplitterService splitterService;
    @Mock
    private OrderUtil orderUtil;
    @Mock
    private SnsPublishService snsPublishService;
    @InjectMocks
    @Spy
    private SalesOrderProcessService salesOrderProcessService;

    @Test
    void testHandleShopOrdersReceived() throws JsonProcessingException {
        String rawMessage = readResource("examples/coreOrderMessage.json");
        var sqsMessage = objectMapperConfig.objectMapper().readValue(rawMessage, SqsMessage.class);
        var order = objectMapperConfig.objectMapper().readValue(sqsMessage.getBody(), Order.class);
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);

        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .rawMessage(rawMessage)
                .message(order)
                .build();

        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);
        when(salesOrderService.checkOrderNotExists(eq(salesOrder.getOrderNumber()))).thenReturn(true);
        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);
        when(splitterService.splitSalesOrder(any())).thenReturn(Collections.singletonList(SalesOrderSplit.regularOrder(salesOrder)));

        salesOrderProcessService.handleShopOrdersReceived(messageWrapper);

        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).createSalesOrder(salesOrder);
        verify(salesOrderService).checkOrderNotExists(salesOrder.getOrderNumber());
    }

    @DisplayName("Test Handle Shop Orders Received Splitted Orders")
    @Test
    @SneakyThrows
    void testHandleShopOrdersReceivedSplittedOrders() {
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        var sqsMessage = objectMapperConfig.objectMapper().readValue(orderRawMessage, SqsMessage.class);
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        order.getOrderHeader().setPlatform(BRAINCRAFT);
        order.getOrderRows().get(0).setGenart("10040");
        order.getOrderRows().get(1).setGenart("test");

        var regularOrder = getSalesOrder((createRegularOrder()));
        var splittedOrder = getSalesOrder(createSplittedOrder());

        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .rawMessage(orderRawMessage)
                .message(order)
                .sqsMessage(sqsMessage)
                .build();

        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);
        when(salesOrderService.checkOrderNotExists(eq(regularOrder.getLatestJson().getOrderHeader().getOrderNumber()))).thenReturn(true);
        when(salesOrderService.checkOrderNotExists(eq(splittedOrder.getLatestJson().getOrderHeader().getOrderNumber()))).thenReturn(true);
        when(salesOrderService.createSalesOrder(eq(regularOrder))).thenReturn(regularOrder);
        when(salesOrderService.createSalesOrder(eq(splittedOrder))).thenReturn(splittedOrder);
        when(splitterService.splitSalesOrder(any())).thenReturn(
                List.of(SalesOrderSplit.regularOrder(regularOrder), SalesOrderSplit.regularOrder(splittedOrder)));

        salesOrderProcessService.handleShopOrdersReceived(messageWrapper);

        verify(camundaHelper).createOrderProcess(eq(regularOrder), any(Messages.class));
        verify(camundaHelper).createOrderProcess(eq(splittedOrder), any(Messages.class));
        verify(salesOrderService).createSalesOrder(regularOrder);
        verify(salesOrderService).createSalesOrder(splittedOrder);
        verify(salesOrderService).checkOrderNotExists(regularOrder.getOrderNumber());
        verify(salesOrderService).checkOrderNotExists(splittedOrder.getOrderNumber());
    }

    @Test
    void testHandleShopOrdersReceivedDuplicatedOrder() throws JsonProcessingException {
        String rawMessage = readResource("examples/coreOrderMessage.json");
        var sqsMessage = objectMapperConfig.objectMapper().readValue(rawMessage, SqsMessage.class);
        var order = objectMapperConfig.objectMapper().readValue(sqsMessage.getBody(), Order.class);
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);

        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .rawMessage(rawMessage)
                .message(order)
                .build();

        when(salesOrderService.checkOrderNotExists(eq("524001240"))).thenReturn(false);
        when(splitterService.splitSalesOrder(any())).thenReturn(Collections.singletonList(SalesOrderSplit.regularOrder(salesOrder)));

        salesOrderProcessService.handleShopOrdersReceived(messageWrapper);

        verify(camundaHelper, never()).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).checkOrderNotExists("524001240");
    }

    @Test
    void testHandleShopOrdersReceivedNoOrderRows() throws JsonProcessingException {

        String rawMessage = readResource("examples/coreOrderMessage.json");
        var sqsMessage = objectMapperConfig.objectMapper().readValue(rawMessage, SqsMessage.class);
        var order = objectMapperConfig.objectMapper().readValue(sqsMessage.getBody(), Order.class);
        order.setOrderRows(List.of());
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setLatestJson(order);
        salesOrder.setRecurringOrder(false);

        MessageWrapper<Order> messageWrapper = MessageWrapper.<Order>builder()
                .rawMessage(rawMessage)
                .message(order)
                .build();

        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(false);
        doNothing().when(snsPublishService).publishOrderCreated(anyString());
        when(salesOrderService.checkOrderNotExists(eq(salesOrder.getOrderNumber()))).thenReturn(true);
        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);
        when(splitterService.splitSalesOrder(any())).thenReturn(Collections.singletonList(SalesOrderSplit.regularOrder(salesOrder)));

        salesOrderProcessService.handleShopOrdersReceived(messageWrapper);

        verify(camundaHelper, never()).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).createSalesOrder(salesOrder);
        verify(salesOrderService).checkOrderNotExists(salesOrder.getOrderNumber());
    }

    private Order createRegularOrder() {
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(K24.getName());
        order.getOrderHeader().setPlatform(BRAINCRAFT);
        order.getOrderRows().get(0).setGenart("test");
        order.getOrderRows().remove(1);

        return order;
    }

    private Order createSplittedOrder() {
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderNumber(order.getOrderHeader().getOrderNumber() + "-1");
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        order.getOrderHeader().setPlatform(BRAINCRAFT);
        order.getOrderRows().get(0).setGenart("10040");
        order.getOrderRows().remove(1);
        return order;
    }
}