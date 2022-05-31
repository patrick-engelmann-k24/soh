package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
import org.assertj.core.util.Lists;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.runtime.CorrelationHandlerResult;
import org.camunda.bpm.engine.impl.runtime.MessageCorrelationResultImpl;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getInvoiceMsg;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderRowServiceTest {

    private static final String ANY_PROCESS_INSTANCE_ID = UUID.randomUUID().toString();

    @Mock
    private CamundaHelper camundaHelper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private TimedPollingService timedPollingService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private OrderUtil orderUtil;

    @InjectMocks
    private SalesOrderRowService salesOrderRowService;

    @Test
    void testCancellingAnOrderRow() {
        final String processId = prepareOrderProcessMocks();
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();

        Order latestJson = salesOrder.getLatestJson();
        var orderRowIds = latestJson.getOrderRows().stream().map(OrderRows::getSku).collect(toList());
        final var originalOrderRowCount = orderRowIds.size();
        final var indexToCancel = 0;
        OrderRows orderRowsToCancel = latestJson.getOrderRows().get(indexToCancel);

        when(salesOrderService.getOrderNumberListByOrderGroupId(orderNumber, orderRowsToCancel.getSku())).thenReturn(List.of(salesOrder.getOrderNumber()));
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        when(runtimeService.getVariable(any(), any())).thenReturn(orderRowIds);
        when(camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())).thenReturn(true);

        salesOrderRowService.cancelOrderRows(salesOrder.getOrderNumber(), Lists.newArrayList(orderRowsToCancel.getSku()));

        checkTotalsValues(salesOrder.getLatestJson().getOrderHeader().getTotals());
        assertThat(orderRowIds.size()).isEqualTo(originalOrderRowCount - 1);
        verify(salesOrderService).save(
                argThat(order -> order.getLatestJson().getOrderRows().get(indexToCancel).getIsCancelled()),
                eq(ORDER_ROW_CANCELLED)
        );

        verify(runtimeService).getVariable(processId, ORDER_ROWS.getName());
        verify(runtimeService).setVariable(processId, ORDER_ROWS.getName(), orderRowIds);
        verify(snsPublishService).publishOrderRowCancelled(any(), any());
    }

    private void checkTotalsValues(Totals totals) {

        assertEquals(BigDecimal.valueOf(91), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.valueOf(77), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(87), totals.getTotalDiscountGross());
        assertEquals(BigDecimal.valueOf(69), totals.getTotalDiscountNet());
        assertEquals(BigDecimal.valueOf(4), totals.getGrandTotalGross());
        assertEquals(BigDecimal.valueOf(8), totals.getGrandTotalNet());
        assertEquals(BigDecimal.valueOf(4), totals.getPaymentTotal());
    }

    private String prepareOrderProcessMocks() {
        var processInstance = mock(ProcessInstance.class);
        final var processId = "123";
        when(processInstance.getId()).thenReturn(processId);
        var processInstanceQuery = mock(ProcessInstanceQuery.class);
        when(runtimeService.createProcessInstanceQuery()).thenReturn(processInstanceQuery);
        when(processInstanceQuery.processDefinitionKey(any())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.variableValueEquals(any(), any())).thenReturn(processInstanceQuery);
        when(processInstanceQuery.singleResult()).thenReturn(processInstance);

        return processId;
    }

    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreatedDuplication() {

        String rawEventMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");
        rawEventMessage = rawEventMessage.replace("InvoiceNumber\\\": \\\"10", "InvoiceNumber\\\": \\\"11111");
        var invoiceMsg = getInvoiceMsg(rawEventMessage);
        var orderNumber = invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var newOrderNumber = createOrderNumberInSOH(orderNumber, invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());

        SalesOrder salesOrder = createSubsequentSalesOrder(orderNumber, "10");
        salesOrder.setInvoiceEvent(invoiceMsg);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(orderUtil.createOrderNumberInSOH(any(), any())).thenReturn(newOrderNumber);

        salesOrderRowService.handleMigrationSubsequentOrder(invoiceMsg, salesOrder);

        verify(snsPublishService).publishMigrationOrderCreated(newOrderNumber);
        var event = EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(invoiceMsg);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(event);
    }

    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreatedNewSubsequentOrder() {

        String rawEventMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");
        var invoiceMsg = getInvoiceMsg(rawEventMessage);
        var orderNumber = invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var newOrderNumber = createOrderNumberInSOH(orderNumber, invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
        when(orderUtil.createOrderNumberInSOH(any(), any())).thenReturn(newOrderNumber);

        SalesOrder salesOrder = createSubsequentSalesOrder(orderNumber, "");
        salesOrder.setInvoiceEvent(invoiceMsg);

        salesOrderRowService.handleMigrationSubsequentOrder(invoiceMsg, salesOrder);

        verify(snsPublishService, never()).publishMigrationOrderCreated(newOrderNumber);
        var event = EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(invoiceMsg);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(event);
    }

    @Test
    void testCorrelateOrderRowMessageFilteredOIn() {

        var executionEntity = new ExecutionEntity();
        executionEntity.setProcessInstanceId(ANY_PROCESS_INSTANCE_ID);
        var messageCorrelationResult =
                new MessageCorrelationResultImpl(CorrelationHandlerResult.matchedExecution(executionEntity));

        when(salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(anyString(), anyString()))
                .thenReturn(List.of("fake_order_number"));
        when(camundaHelper.correlateMessageForOrderRowProcess(any(), any(), any(), any()))
                .thenReturn(messageCorrelationResult);

        salesOrderRowService.correlateOrderRowMessage(
                ROW_TRANSMITTED_TO_LOGISTICS,
                "fake_order_number",
                "fake_item_sku",
                "",
                "",
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);

        verify(salesOrderService).getOrderNumberListByOrderGroupIdAndFilterNotCancelled(anyString(), anyString());
        verify(camundaHelper).correlateMessageForOrderRowProcess(
                eq(ROW_TRANSMITTED_TO_LOGISTICS),
                eq("fake_order_number"),
                eq(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                eq("fake_item_sku"));

    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForCorrelateOrderRowMessage")
    void testCorrelateOrderRowMessageFilteredOut(String orderItemSku) {

        salesOrderRowService.correlateOrderRowMessage(
                ROW_TRANSMITTED_TO_LOGISTICS,
                "fake_order_number",
                orderItemSku,
                "",
                "",
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);

        verify(salesOrderService, never()).getOrderNumberListByOrderGroupIdAndFilterNotCancelled(anyString(), anyString());
        verify(camundaHelper, never()).correlateMessageForOrderRowProcess(
                eq(ROW_TRANSMITTED_TO_LOGISTICS),
                eq("fake_order_number"),
                eq(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                eq(orderItemSku));
    }

    private static Stream<Arguments> provideArgumentsForCorrelateOrderRowMessage() {

        return Stream.of(Arguments.of("MARK-0001"),
                Arguments.of("KBA"),
                Arguments.of("KBA2"),
                Arguments.of("KBA3"),
                Arguments.of("90101083"));
    }

    private SalesOrder createSubsequentSalesOrder(String orderNumber, String invoiceNumber) {
        String rawOrderMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(rawOrderMessage);
        order.getOrderHeader().setOrderNumber(orderNumber + invoiceNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        return getSalesOrder(order);
    }
}