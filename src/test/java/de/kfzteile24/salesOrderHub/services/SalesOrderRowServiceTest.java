package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationItem;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationMessage;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
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
import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private SnsPublishService snsPublishService;

    @InjectMocks
    private SalesOrderRowService salesOrderRowService;

    @Test
    public void cancellingAnOrderRow() {
        final String processId = prepareOrderProcessMocks();
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();

        Order latestJson = salesOrder.getLatestJson();
        var orderRowIds = latestJson.getOrderRows().stream().map(OrderRows::getSku).collect(toList());
        final var originalOrderRowCount = orderRowIds.size();
        final var indexToCancel = 0;
        OrderRows orderRowsToCancel = latestJson.getOrderRows().get(indexToCancel);
        CoreCancellationMessage coreCancellationMessage = getCoreCancellationMessage(salesOrder, orderRowsToCancel);

        when(salesOrderService.getOrderNumberListByOrderGroupId(orderNumber, orderRowsToCancel.getSku())).thenReturn(List.of(salesOrder.getOrderNumber()));
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));
        when(runtimeService.getVariable(any(), any())).thenReturn(orderRowIds);
        when(camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())).thenReturn(true);

        salesOrderRowService.cancelOrderRows(coreCancellationMessage);

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

    private CoreCancellationMessage getCoreCancellationMessage(SalesOrder salesOrder, OrderRows orderRowsToCancel) {
        CoreCancellationItem coreCancellationItem = CoreCancellationItem.builder()
                .sku(orderRowsToCancel.getSku())
                .quantity(orderRowsToCancel.getQuantity().intValue())
                .build();
        List<CoreCancellationItem> items = Lists.list(coreCancellationItem);

        return CoreCancellationMessage.builder()
                .orderNumber(salesOrder.getOrderNumber())
                .items(items)
                .build();
    }

    private void checkTotalsValues(Totals totals) {

        assertEquals(BigDecimal.valueOf(90), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.valueOf(79), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(90), totals.getTotalDiscountGross());
        assertEquals(BigDecimal.valueOf(79), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(0), totals.getGrandTotalGross());
        assertEquals(BigDecimal.valueOf(0), totals.getGrandTotalNet());
        assertEquals(BigDecimal.valueOf(0), totals.getPaymentTotal());
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