package de.kfzteile24.salesOrderHub.services;


import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
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
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents.ROW_DELIVERED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents.ROW_PICKED_UP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents.TRACKING_ID_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.CLICK_COLLECT;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.DIRECT_DELIVERY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.EXPRESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UnusedPrivateMethod", "PMD.UnusedPrivateField"})
class SalesOrderRowServiceTest {
    @Mock
    private CamundaHelper camundaHelper;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private HistoryService historyService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private SalesOrderRowService salesOrderRowService;

    @ParameterizedTest
    @MethodSource("provideCheckCancellationPossibleTestData")
    public void orderRowsCanOnlyBeCancelledUntilAShipmentMethodSpecificState(ShipmentMethod shipmentMethod,
                                                                             RowEvents state) {
        when(camundaHelper.hasNotPassed(any(), any())).thenReturn(true);

        final var processId = "123";
        final var isCancellationPossible = salesOrderRowService.checkOrderRowCancellationPossible(processId, shipmentMethod.getName());

        assertThat(isCancellationPossible).isTrue();
        verify(camundaHelper).hasNotPassed(eq(processId), eq(state.getName()));
    }

    @Test
    public void unknownShipmentMethodsCannotBeCancelled() {
        final var isCancellationPossible = salesOrderRowService.checkOrderRowCancellationPossible("123", "pigeon");

        assertThat(isCancellationPossible).isFalse();
    }

    @Test
    public void cancellingAnOrderRowWithoutSubprocessesIsHandledCorrectly() {
        final String processId = prepareMocks();
        final var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));

        var orderRowIds = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var originalOrderRowCount = orderRowIds.size();

        when(runtimeService.getVariable(any(), any())).thenReturn(orderRowIds);

        final var indexToCancel = 0;
        final var orderRowToCancel = salesOrder.getLatestJson().getOrderRows().get(indexToCancel);
        salesOrderRowService.cancelOrderRowWithoutSubprocess(orderNumber, orderRowToCancel.getSku());

        assertThat(orderRowIds.size()).isEqualTo(originalOrderRowCount - 1);
        verify(salesOrderService).save(
                argThat(order -> order.getLatestJson().getOrderRows().get(indexToCancel).getIsCancelled()),
                eq(ORDER_ROW_CANCELLED)
        );

        verify(runtimeService).getVariable(processId, ORDER_ROWS.getName());
        verify(runtimeService).setVariable(processId, ORDER_ROWS.getName(), orderRowIds);
        verify(snsPublishService).publishOrderRowsCancelled(eq(salesOrder.getLatestJson()),
                eq(List.of(orderRowToCancel)), eq(false));
        verifyNoMoreInteractions(snsPublishService);
    }

    @Test
    public void cancellingTheLastOrderRowWithoutSubprocessesCancelsTheCompleteOrderIncludingVirtualItems() {
        final String processId = prepareMocks();
        final var salesOrder = SalesOrderUtil.createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        var orderRows = salesOrder.getLatestJson().getOrderRows();
        assertThat(orderRows.size()).isEqualTo(3);
        final var virtualItemIndex = 0;
        assertThat(orderRows.get(virtualItemIndex).getIsCancelled()).isFalse();
        orderRows.get(1).setIsCancelled(true);

        final var orderNumber = salesOrder.getOrderNumber();
        when(salesOrderService.getOrderByOrderNumber(orderNumber)).thenReturn(Optional.of(salesOrder));

        final var indexToCancel = 2;
        var orderRowIds = new ArrayList<>(Collections.singleton(orderRows.get(indexToCancel).getSku()));
        when(runtimeService.getVariable(any(), any())).thenReturn(orderRowIds);
        final var messageCorrelationBuilder = mock(MessageCorrelationBuilder.class);
        final var messageCorrelationResult = mock(MessageCorrelationResult.class);
        when(runtimeService.createMessageCorrelation(any())).thenReturn(messageCorrelationBuilder);
        when(messageCorrelationBuilder.processInstanceVariableEquals(any(), any())).thenReturn(messageCorrelationBuilder);
        when(messageCorrelationBuilder.correlateWithResult()).thenReturn(messageCorrelationResult);

        final var orderRowToCancel = orderRows.get(indexToCancel);
        salesOrderRowService.cancelOrderRowWithoutSubprocess(orderNumber, orderRowToCancel.getSku());

        assertThat(orderRowIds).isEmpty();
        assertThat(orderRows.get(virtualItemIndex).getIsCancelled()).isTrue();
        verify(salesOrderService).save(
                argThat(order -> order.getLatestJson().getOrderRows().stream().allMatch(OrderRows::getIsCancelled)),
                eq(ORDER_ROW_CANCELLED)
        );
        verify(runtimeService).getVariable(processId, ORDER_ROWS.getName());
        verify(runtimeService).setVariable(processId, ORDER_ROWS.getName(), emptyList());
        verify(runtimeService).createMessageCorrelation(eq(Messages.ORDER_CANCELLATION_RECEIVED.getName()));
        verifyNoInteractions(snsPublishService);
    }

    private String prepareMocks() {
        when(camundaHelper.isShipped(any())).thenAnswer((Answer<Boolean>) invocation -> {
            final var shippingType = (String) invocation.getArgument(0);
            return !shippingType.equals(ShipmentMethod.NONE.getName());
        });
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

    private static Stream<Arguments> provideCheckCancellationPossibleTestData() {
        return Stream.of(
                Arguments.of(REGULAR, TRACKING_ID_RECEIVED),
                Arguments.of(EXPRESS, TRACKING_ID_RECEIVED),
                Arguments.of(CLICK_COLLECT, ROW_PICKED_UP),
                Arguments.of(DIRECT_DELIVERY, ROW_DELIVERED)
        );
    }
}