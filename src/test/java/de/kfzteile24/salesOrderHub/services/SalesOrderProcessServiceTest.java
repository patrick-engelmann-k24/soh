package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.helper.CustomValidator;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
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
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
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
    @Mock
    private CustomValidator customValidator;

    @InjectMocks
    @Spy
    private SalesOrderProcessService salesOrderProcessService;

    @Test
    void testHandleShopOrdersReceived() {
        var message = getObjectByResource("coreOrderMessage.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();
        SalesOrder salesOrder = getSalesOrder(message);
        salesOrder.setRecurringOrder(false);

        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);
        when(salesOrderService.checkOrderNotExists(salesOrder.getOrderNumber())).thenReturn(true);
        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);
        when(splitterService.splitSalesOrder(any(), any())).thenReturn(Collections.singletonList(SalesOrderSplit.regularOrder(salesOrder)));

        salesOrderProcessService.handleShopOrdersReceived(message, messageWrapper);

        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).createSalesOrder(salesOrder);
        verify(salesOrderService).checkOrderNotExists(salesOrder.getOrderNumber());
        verify(salesOrderService).enrichInitialOrder(message);
    }

    @DisplayName("Test Handle Shop Orders Received Splitted Orders")
    @Test
    @SneakyThrows
    void testHandleShopOrdersReceivedSplittedOrders() {
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();
        message.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        message.getOrderHeader().setPlatform(BRAINCRAFT);
        message.getOrderRows().get(0).setGenart("10040");
        message.getOrderRows().get(1).setGenart("test");

        var regularOrder = getSalesOrder((createRegularOrder()));
        var splittedOrder = getSalesOrder(createSplittedOrder());

        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);
        when(salesOrderService.checkOrderNotExists(regularOrder.getLatestJson().getOrderHeader().getOrderNumber())).thenReturn(true);
        when(salesOrderService.checkOrderNotExists(splittedOrder.getLatestJson().getOrderHeader().getOrderNumber())).thenReturn(true);
        when(salesOrderService.createSalesOrder(regularOrder)).thenReturn(regularOrder);
        when(salesOrderService.createSalesOrder(splittedOrder)).thenReturn(splittedOrder);
        when(splitterService.splitSalesOrder(any(), any())).thenReturn(
                List.of(SalesOrderSplit.regularOrder(regularOrder), SalesOrderSplit.regularOrder(splittedOrder)));

        salesOrderProcessService.handleShopOrdersReceived(message, messageWrapper);

        verify(camundaHelper).createOrderProcess(eq(regularOrder), any(Messages.class));
        verify(camundaHelper).createOrderProcess(eq(splittedOrder), any(Messages.class));
        verify(salesOrderService).createSalesOrder(regularOrder);
        verify(salesOrderService).createSalesOrder(splittedOrder);
        verify(salesOrderService).checkOrderNotExists(regularOrder.getOrderNumber());
        verify(salesOrderService).checkOrderNotExists(splittedOrder.getOrderNumber());
        verify(salesOrderService).enrichInitialOrder(message);
    }

    @Test
    void testHandleShopOrdersReceivedDuplicatedOrder() {
        var message = getObjectByResource("coreOrderMessage.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();

        SalesOrder salesOrder = getSalesOrder(message);
        salesOrder.setRecurringOrder(false);

        when(salesOrderService.checkOrderNotExists("524001240")).thenReturn(false);
        when(splitterService.splitSalesOrder(any(), any())).thenReturn(Collections.singletonList(SalesOrderSplit.regularOrder(salesOrder)));

        salesOrderProcessService.handleShopOrdersReceived(message,  messageWrapper);

        verify(camundaHelper, never()).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).checkOrderNotExists("524001240");
        verify(salesOrderService).enrichInitialOrder(message);
    }

    @Test
    void testHandleShopOrdersReceivedNoOrderRows() {

        var message = getObjectByResource("coreOrderMessage.json", Order.class);
        var messageWrapper = MessageWrapper.builder().build();
        message.setOrderRows(List.of());
        SalesOrder salesOrder = getSalesOrder(message);
        salesOrder.setLatestJson(message);
        salesOrder.setRecurringOrder(false);

        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(false);
        doNothing().when(snsPublishService).publishOrderCreated(anyString());
        when(salesOrderService.checkOrderNotExists(salesOrder.getOrderNumber())).thenReturn(true);
        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);
        when(splitterService.splitSalesOrder(any(), any())).thenReturn(Collections.singletonList(SalesOrderSplit.regularOrder(salesOrder)));


        salesOrderProcessService.handleShopOrdersReceived(message,  messageWrapper);

        verify(camundaHelper, never()).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).createSalesOrder(salesOrder);
        verify(salesOrderService).checkOrderNotExists(salesOrder.getOrderNumber());
        verify(salesOrderService).enrichInitialOrder(message);
    }

    private Order createRegularOrder() {
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        message.getOrderHeader().setOrderFulfillment(K24.getName());
        message.getOrderHeader().setPlatform(BRAINCRAFT);
        message.getOrderRows().get(0).setGenart("test");
        message.getOrderRows().remove(1);

        return message;
    }

    private Order createSplittedOrder() {
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        message.getOrderHeader().setOrderNumber(message.getOrderHeader().getOrderNumber() + "-1");
        message.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        message.getOrderHeader().setPlatform(BRAINCRAFT);
        message.getOrderRows().get(0).setGenart("10040");
        message.getOrderRows().remove(1);
        return message;
    }
}