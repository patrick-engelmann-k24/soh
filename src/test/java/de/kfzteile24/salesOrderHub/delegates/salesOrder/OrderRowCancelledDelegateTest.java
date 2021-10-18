package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.delegates.salesOrder.row.OrderRowCancelledDelegate;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRowCancelledDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private OrderRowCancelledDelegate orderRowCancelledDelegate;

    @Test
    @SneakyThrows(Exception.class)
    public void theDelegateUpdatesTheSalesOrderPublishesAnOrderRowsCancelledEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var originalOrderRowCount = salesOrder.getLatestJson().getOrderRows().size();
        final var skuToCancel = salesOrder.getLatestJson().getOrderRows().get(0).getSku();
        when(delegateExecution.getVariable(ORDER_ROW_ID.getName())).thenReturn(skuToCancel);

        when(salesOrderService.getOrderByOrderNumber(expectedOrderNumber)).thenReturn(Optional.of(salesOrder));

        orderRowCancelledDelegate.execute(delegateExecution);

        verify(salesOrderService).save(
                argThat(order ->
                        order.getLatestJson().getOrderRows().size() == originalOrderRowCount &&
                        onlyTheExpectedRowIsCancelled(order.getLatestJson().getOrderRows(), skuToCancel)),
                eq(Action.ORDER_ROW_CANCELLED));


        verify(snsPublishService).publishOrderRowsCancelled(
                argThat(order -> order.getOrderRows().size() == originalOrderRowCount &&
                        onlyTheExpectedRowIsCancelled(order.getOrderRows(), skuToCancel)),
                argThat(orderRows -> orderRows.size() == 1 && orderRows.get(0).getSku().equals(skuToCancel)),
                eq(false));
    }

    private boolean onlyTheExpectedRowIsCancelled(List<OrderRows> orderRows, String skuToCancel) {
        return orderRows.stream()
                .filter(orderRow -> orderRow.getIsCancelled() && orderRow.getSku().equals(skuToCancel))
                .count() == 1;

    }
}