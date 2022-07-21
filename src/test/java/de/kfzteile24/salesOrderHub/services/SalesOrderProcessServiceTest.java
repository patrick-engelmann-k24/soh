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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
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
}