package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @Mock
    private OrderUtil orderUtil;

    @InjectMocks
    private SalesOrderRowService salesOrderRowService;

    @Test
    void testCancelOrderProcessIfFullyCancelled() {

        var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.getLatestJson().getOrderRows().forEach(orderRows -> orderRows.setIsCancelled(true));

        try (MockedStatic<ShipmentMethod> mockStatic = mockStatic(ShipmentMethod.class)) {
            mockStatic.when(() -> ShipmentMethod.isShipped(anyString())).thenReturn(true);
        }

        salesOrderRowService.cancelOrderProcessIfFullyCancelled(salesOrder);

        verify(salesOrderService).save(argThat(SalesOrder::isCancelled), eq(ORDER_CANCELLED));
    }

    @Test
    void testCancelOrderProcessIfNotFullyCancelled() {

        var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.getLatestJson().getOrderRows().get(0).setIsCancelled(true);

        salesOrderRowService.cancelOrderProcessIfFullyCancelled(salesOrder);

        verify(salesOrderService, never()).save(argThat(SalesOrder::isCancelled), eq(ORDER_CANCELLED));
    }
}