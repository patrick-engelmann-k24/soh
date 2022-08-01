package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
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
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getInvoiceMsg;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private static final String ANY_PROCESS_ID = UUID.randomUUID().toString();

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


    private SalesOrderUtil util;

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

    @ParameterizedTest
    @MethodSource("provideArgumentsForCorrelateOrderRowMessageForNonVirtualItems")
    void testCorrelateOrderRowMessageFilteredOIn(String orderItemSku) {

        var executionEntity = new ExecutionEntity();
        executionEntity.setId(ANY_PROCESS_ID);
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
                orderItemSku,
                "",
                "",
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);

        verify(salesOrderService).getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                eq("fake_order_number"),
                eq(orderItemSku));
        verify(camundaHelper).correlateMessageForOrderRowProcess(
                eq(ROW_TRANSMITTED_TO_LOGISTICS),
                eq("fake_order_number"),
                eq(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                eq(orderItemSku));

    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForCorrelateOrderRowMessageForVirtualItems")
    void testCorrelateOrderRowMessageFilteredOut(String orderItemSku) {

        salesOrderRowService.correlateOrderRowMessage(
                ROW_TRANSMITTED_TO_LOGISTICS,
                "fake_order_number",
                orderItemSku,
                "",
                "",
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);

        verify(salesOrderService, never()).getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                eq("fake_order_number"),
                eq(orderItemSku));
        verify(camundaHelper, never()).correlateMessageForOrderRowProcess(
                eq(ROW_TRANSMITTED_TO_LOGISTICS),
                eq("fake_order_number"),
                eq(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                eq(orderItemSku));
    }

    @Test
    void testHandleParcelShippedEvent() {
        final SalesOrder salesOrder1 = createSalesOrder(
                null,
                "sku1"
        );
        final SalesOrder salesOrder2 = createSalesOrder(
                LocalDateTime.of(2022, 2, 1, 1, 0, 0),
                "sku1", "sku2", "sku3"
        );
        final SalesOrder salesOrder3 = createSalesOrder(
                LocalDateTime.of(2022, 3, 1, 1, 0, 0),
                "sku4", "sku5"
        );
        var orderNumber = salesOrder1.getOrderNumber();
        var event = ParcelShipped.builder()
                .orderNumber(orderNumber)
                .deliveryNoteNumber("delivery-note-12345")
                .trackingNumber("tracking-12345")
                .trackingLink("http://tacking-link")
                .logisticsPartnerName("dhl")
                .articleItemsDtos(Collections.singleton(
                        ArticleItemsDto.builder()
                                .number("sku1")
                                .quantity(BigDecimal.ONE)
                                .description("sku name 1")
                                .isDeposit(false)
                                .build()
                ))
                .build();
        when(salesOrderService.getOrderByOrderGroupId(eq(orderNumber))).thenReturn(List.of(salesOrder3, salesOrder2, salesOrder1));

        salesOrderRowService.handleParcelShippedEvent(event);

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder2, getTrackingLinks(event));
    }

    @Test
    void testHandleParcelShippedEventWhenNoSalesOrder() {
        var orderNumber = "123456789";
        var event = ParcelShipped.builder()
                .orderNumber(orderNumber)
                .deliveryNoteNumber("delivery-note-12345")
                .trackingNumber("tracking-12345")
                .trackingLink("http://tacking-link")
                .logisticsPartnerName("dhl")
                .articleItemsDtos(Collections.singleton(
                        ArticleItemsDto.builder()
                                .number("sku1")
                                .quantity(BigDecimal.ONE)
                                .description("sku name 1")
                                .isDeposit(false)
                                .build()
                ))
                .build();
        when(salesOrderService.getOrderByOrderGroupId(eq(orderNumber))).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> salesOrderRowService.handleParcelShippedEvent(event))
                .isExactlyInstanceOf(NotFoundException.class)
                .hasMessageContaining(MessageFormat.format("There is no sales order including all article number " +
                        "in the parcel shipped event. " +
                        "OrderNumber: {0}, " +
                        "DeliveryNoteNumber: {1}, " +
                        "articleItemsList: {2}",
                        event.getOrderNumber(),
                        event.getDeliveryNoteNumber(),
                        "[sku1]"));
    }

    private List<TrackingLink> getTrackingLinks(ParcelShipped event) {
        return List.of(TrackingLink.builder()
                .url(event.getTrackingLink())
                .orderItems(event.getArticleItemsDtos().stream().map(ArticleItemsDto::getNumber).collect(toList()))
                .build());
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

    private static Stream<Arguments> provideArgumentsForCorrelateOrderRowMessageForNonVirtualItems() {

        return Stream.of(
                Arguments.of("901-1083"),
                Arguments.of("9013"),
                Arguments.of("aKBA"),
                Arguments.of("aaaMARK-0001"));
    }

    private static Stream<Arguments> provideArgumentsForCorrelateOrderRowMessageForVirtualItems() {

        return Stream.of(
                Arguments.of("MARK-0001"),
                Arguments.of("KBA"),
                Arguments.of("KBA2"),
                Arguments.of("KBA"),
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