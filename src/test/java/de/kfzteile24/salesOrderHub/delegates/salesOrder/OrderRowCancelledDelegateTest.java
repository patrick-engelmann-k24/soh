package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.delegates.salesOrder.row.OrderRowCancelledDelegate;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRowCancelledDelegateTest {
    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SalesOrderRowService salesOrderRowService;

    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private OrderRowCancelledDelegate orderRowCancelledDelegate;

    @Test
    @SneakyThrows(Exception.class)
    public void testOrderRowCancelledDelegate() {
        final var expectedOrderNumber = "123";
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var skuToCancel = salesOrder.getLatestJson().getOrderRows().get(0).getSku();
        when(delegateExecution.getVariable(ORDER_ROW_ID.getName())).thenReturn(skuToCancel);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        doNothing().when(salesOrderRowService).cancelOrderProcessIfFullyCancelled(salesOrder);

        orderRowCancelledDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderRowCancelled(expectedOrderNumber, skuToCancel);
    }
}