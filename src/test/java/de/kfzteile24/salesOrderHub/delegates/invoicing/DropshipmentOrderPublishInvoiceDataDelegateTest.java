package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DropshipmentOrderPublishInvoiceDataDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private MetricsHelper metricsHelper;

    @InjectMocks
    private DropshipmentOrderPublishInvoiceDataDelegate dropshipmentOrderPublishInvoiceDataDelegate;

    @Test
    @SneakyThrows(Exception.class)
    void testPublishInvoiceDataDelegate() {
        final var expectedOrderNumber = "123";
        final var expectedOrder = SalesOrder.builder().orderNumber(expectedOrderNumber).invoiceEvent(
                CoreSalesInvoiceCreatedMessage.builder().build())
                .latestJson(Order.builder().orderHeader(OrderHeader.builder().build()).build())
                .build();
        expectedOrder.setId(UUID.randomUUID());
        when(delegateExecution.getVariable(Variables.SALES_ORDER_ID.getName())).thenReturn(expectedOrder.getId());
        when(salesOrderService.getOrderById(expectedOrder.getId())).thenReturn(Optional.of(expectedOrder));
        dropshipmentOrderPublishInvoiceDataDelegate.execute(delegateExecution);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(expectedOrder.getInvoiceEvent()));
    }
}
