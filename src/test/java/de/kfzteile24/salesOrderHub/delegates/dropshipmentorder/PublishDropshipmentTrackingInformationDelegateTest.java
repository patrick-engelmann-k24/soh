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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishDropshipmentTrackingInformationDelegateTest {

    @Mock
    private DelegateExecution delegateExecution;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PublishDropshipmentTrackingInformationDelegate publishDropshipmentTrackingInformationDelegate;

    @Test
    void testPublishDropshipmentTrackingInformationDelegate() throws Exception {
        final var expectedOrderNumber = "123";

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber(expectedOrderNumber);
        final var trackingLinks = Stream.of("http://abc1", "http://abc2")
                .map(link -> TrackingLink.builder()
                        .url(link)
                        .build()
                ).collect(Collectors.toList());
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(expectedOrderNumber);
        when(delegateExecution.getVariable(TRACKING_LINKS.getName())).thenReturn(List.of("{\"url\":\"http://abc1\"}", "{\"url\":\"http://abc2\"}"));
        when(objectMapper.readValue("{\"url\":\"http://abc1\"}", TrackingLink.class)).thenReturn(trackingLinks.get(0));
        when(objectMapper.readValue("{\"url\":\"http://abc2\"}", TrackingLink.class)).thenReturn(trackingLinks.get(1));
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));

        publishDropshipmentTrackingInformationDelegate.execute(delegateExecution);

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(salesOrder, trackingLinks);

    }
}