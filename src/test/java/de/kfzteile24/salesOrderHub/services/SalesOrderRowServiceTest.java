package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationItem;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationMessage;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;

import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesOrderRowServiceTest {

    @Mock
    private CamundaHelper camundaHelper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private SnsPublishService snsPublishService;

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
        when(orderUtil.removeCancelledOrderRowsFromLatestJson(any())).thenReturn(salesOrder);

        salesOrderRowService.cancelOrderRows(salesOrder.getOrderNumber(), Lists.newArrayList(orderRowsToCancel.getSku()));

        checkTotalsValues(salesOrder.getLatestJson().getOrderHeader().getTotals());
        assertThat(orderRowIds.size()).isEqualTo(originalOrderRowCount - 1);
        verify(salesOrderService).save(
                argThat(order -> order.getLatestJson().getOrderRows().get(indexToCancel).getIsCancelled()),
                eq(ORDER_ROW_CANCELLED)
        );

        verify(runtimeService).getVariable(processId, ORDER_ROWS.getName());
        verify(runtimeService).setVariable(processId, ORDER_ROWS.getName(), orderRowIds);
        verifyNoInteractions(snsPublishService);
    }

    @Test
    void testHandleDropshipmentPurchaseOrderBooked() {

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();

        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));

        DropshipmentPurchaseOrderBookedMessage message = DropshipmentPurchaseOrderBookedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .purchaseOrderNumber("52.3")
                .booked(true)
                .build();
        salesOrderRowService.handleDropshipmentPurchaseOrderBooked(message);

        assertEquals("52.3", salesOrder.getLatestJson().getOrderHeader().getOrderNumberExternal());
        verify(salesOrderService).save(
                argThat(order -> order.getLatestJson().getOrderHeader().getOrderNumberExternal().equals("52.3")),
                eq(DROPSHIPMENT_PURCHASE_ORDER_BOOKED)
        );

        verifyNoInteractions(snsPublishService);
        verify(salesOrderService, never()).getOrderByOrderGroupId(any());
    }

    @Test
    void testHandleDropshipmentPurchaseOrderBookedFalse() {

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();

        when(salesOrderService.getOrderNumberListByOrderGroupId(eq(orderNumber), any())).thenReturn(List.of(salesOrder.getOrderNumber()));
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.save(any(), eq(DROPSHIPMENT_PURCHASE_ORDER_BOOKED))).thenReturn(salesOrder);
        when(camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())).thenReturn(false);
        when(camundaHelper.checkIfOrderRowProcessExists(eq(salesOrder.getOrderNumber()), any())).thenReturn(false);

        DropshipmentPurchaseOrderBookedMessage message = DropshipmentPurchaseOrderBookedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .purchaseOrderNumber("27.9")
                .booked(false)
                .build();
        salesOrderRowService.handleDropshipmentPurchaseOrderBooked(message);

        assertEquals("27.9", salesOrder.getLatestJson().getOrderHeader().getOrderNumberExternal());
        verify(salesOrderService).save(
                argThat(order -> order.getLatestJson().getOrderHeader().getOrderNumberExternal().equals("27.9")),
                eq(DROPSHIPMENT_PURCHASE_ORDER_BOOKED)
        );

        verify(salesOrderService, times(3)).getOrderNumberListByOrderGroupId(eq(salesOrder.getOrderGroupId()), any());
        verify(salesOrderService, times(3)).save(any(), eq(Action.ORDER_ROW_CANCELLED));
        verifyNoInteractions(snsPublishService);
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
}