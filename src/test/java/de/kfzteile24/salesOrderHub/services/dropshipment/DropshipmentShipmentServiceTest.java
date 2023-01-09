package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.events.shipmentconfirmed.TrackingLink;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentShipmentServiceTest {

    public static final String ANY_PROCESS_ID = RandomStringUtils.randomNumeric(10);

    @InjectMocks
    @Spy
    private DropshipmentShipmentService dropshipmentShipmentService;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private CamundaHelper camundaHelper;
    @Spy
    private ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    void testHandleDropshipmentShipmentConfirmed() {

        val salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setProcessId(ANY_PROCESS_ID);
        salesOrder.setShipped(true);
        val items = salesOrder.getLatestJson().getOrderRows().stream()
                .map(row -> ShipmentItem.builder()
                        .productNumber(row.getSku())
                        .parcelNumber(row.getSku())
                        .serviceProviderName(row.getSku())
                        .trackingLink(row.getSku() + " trackingLink")
                        .build())
                .collect(toUnmodifiableList());

        val message = DropshipmentShipmentConfirmedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .items(items)
                .build();

        when(salesOrderService.getOrderByOrderNumber(message.getSalesOrderNumber())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.save(salesOrder, ORDER_ITEM_SHIPPED)).thenReturn(salesOrder);

        dropshipmentShipmentService.handleDropshipmentShipmentConfirmed(message, MessageWrapper.builder().build());

        verify(salesOrderService).getOrderByOrderNumber(message.getSalesOrderNumber());

        verify(salesOrderService).save(
                argThat(so -> {
                    so.getLatestJson().getOrderRows().forEach(row -> {
                                assertEquals(row.getSku(), row.getShippingProvider());
                                assertTrue(row.getTrackingNumbers().contains(row.getSku()));
                            }
                    );
                    return true;
                }),
                eq(ORDER_ITEM_SHIPPED));

        items.forEach(item -> verify(camundaHelper).startProcessByMessage(
                eq(DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED),
                eq(message.getSalesOrderNumber() + "#" + item.getProductNumber()),
                argThat(variablesMap -> assertProcessVariablesMap(message, item, variablesMap)))
        );
    }

    @SneakyThrows
    private boolean assertProcessVariablesMap(DropshipmentShipmentConfirmedMessage message,
                                              ShipmentItem item, Map<String, Object> variablesMap) {
        assertThat(variablesMap)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        ORDER_NUMBER.getName(), message.getSalesOrderNumber(),
                        ORDER_ROW.getName(), item.getProductNumber(),
                        TRACKING_LINKS.getName(), List.of(objectMapper.writeValueAsString(TrackingLink.builder()
                                .url(item.getTrackingLink())
                                .orderItem(item.getProductNumber())
                                .build())),
                        ORDER_FULLY_SHIPPED.getName(), true));
        return true;
    }
}