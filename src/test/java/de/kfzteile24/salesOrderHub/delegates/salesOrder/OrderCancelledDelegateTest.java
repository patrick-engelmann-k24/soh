package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCancelledDelegateTest {
    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderService salesOrderService;

    @InjectMocks
    private OrderCancelledDelegate orderCancelledDelegate;

    @Test
    @SneakyThrows(Exception.class)
    public void theDelegateUpdatesTheSalesOrderPublishesAnOrderRowsCancelledEvent() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var originalOrderRowCount = salesOrder.getLatestJson().getOrderRows().size();
        when(salesOrderService.getOrderByOrderNumber(expectedOrderNumber)).thenReturn(Optional.of(salesOrder));

        orderCancelledDelegate.execute(delegateExecution);

        verify(salesOrderService).save(
                argThat(order -> allRowsAreCancelled(order.getLatestJson().getOrderRows(), originalOrderRowCount)),
                eq(Action.ORDER_CANCELLED));

        verify(snsPublishService).publishOrderRowsCancelled(
                argThat(order -> allRowsAreCancelled(order.getOrderRows(), originalOrderRowCount)),
                argThat(orderRows -> allRowsAreCancelled(orderRows, originalOrderRowCount)),
                eq(true));
    }

    private boolean allRowsAreCancelled(List<OrderRows> orderRows, int expectedRowCount) {
        return orderRows.stream()
                .filter(OrderRows::getIsCancelled)
                .count() == expectedRowCount;
    }

}