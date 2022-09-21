package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderRowCancelledDelegateTest {
    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private DropshipmentOrderRowCancelledDelegate orderRowCancelledDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testOrderRowCancelledDelegate() {
        final var expectedOrderNumber = "123";

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var skuToCancel = salesOrder.getLatestJson().getOrderRows().get(0).getSku();
        when(delegateExecution.getVariable(ORDER_ROW_ID.getName())).thenReturn(skuToCancel);
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);

        orderRowCancelledDelegate.execute(delegateExecution);

        verify(snsPublishService).publishOrderRowCancelled(expectedOrderNumber, skuToCancel);
    }
}