package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishDropshipmentOrderRowTrackingInformationDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private ObjectMapper objectMapper;


    @InjectMocks
    private PublishDropshipmentOrderRowTrackingInformationDelegate publishDropshipmentOrderRowTrackingInformationDelegate;

    @Test
    void testPublishDropshipmentOrderRowTrackingInformationDelegate() throws Exception {
        final var expectedOrderNumber = "123";
        final var expectedSku = "456";
        final var expectedUrl = "789";
        final var expectedTrackingLink = TrackingLink.builder().url(expectedUrl).build();
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber(expectedOrderNumber);

        when(delegateExecution.getVariable(ORDER_ROW.getName())).thenReturn(expectedSku);
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(TRACKING_LINKS.getName())).thenReturn(Collections.singletonList(expectedUrl));
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(objectMapper.readValue(eq(expectedUrl), eq(TrackingLink.class))).thenReturn(expectedTrackingLink);

        publishDropshipmentOrderRowTrackingInformationDelegate.execute(delegateExecution);

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder, Collections.singletonList(expectedTrackingLink));

    }
}