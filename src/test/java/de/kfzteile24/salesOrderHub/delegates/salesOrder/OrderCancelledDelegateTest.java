package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.email.AmazonEmailService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.never;
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

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private AmazonEmailService amazonEmailService;

    @InjectMocks
    private OrderCancelledDelegate orderCancelledDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testRegularOrderCancelled() {
        final var expectedOrderNumber = "123";
        Order order = Order.builder().build();
        SalesOrder salesOrder = SalesOrder.builder().latestJson(order).build();
        when(salesOrderService.getOrderByOrderNumber(expectedOrderNumber)).thenReturn(Optional.of(salesOrder));
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(orderUtil.isDropshipmentOrder(order)).thenReturn(false);

        orderCancelledDelegate.execute(delegateExecution);

        verify(amazonEmailService, never()).sendOrderCancelledEmail(order);
        verify(snsPublishService).publishOrderCancelled(order);
    }

    @Test
    @SneakyThrows(Exception.class)
    void testDropshipmentOrderCancelled() {
        final var expectedOrderNumber = "123";
        Order order = Order.builder().build();
        SalesOrder salesOrder = SalesOrder.builder().latestJson(order).build();
        when(salesOrderService.getOrderByOrderNumber(expectedOrderNumber)).thenReturn(Optional.of(salesOrder));
        when(delegateExecution.getVariable(Variables.ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(orderUtil.isDropshipmentOrder(order)).thenReturn(true);

        orderCancelledDelegate.execute(delegateExecution);

        verify(amazonEmailService).sendOrderCancelledEmail(order);
        verify(snsPublishService).publishOrderCancelled(order);
    }

}