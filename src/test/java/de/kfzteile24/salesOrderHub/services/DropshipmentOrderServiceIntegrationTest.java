package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.StringUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class DropshipmentOrderServiceIntegrationTest {

    @Autowired
    private DropshipmentOrderService dropshipmentOrderService;
    @SpyBean
    private CamundaHelper camundaHelper;
    @SpyBean
    private SalesOrderService salesOrderService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private SalesOrderInvoiceRepository salesOrderInvoiceRepository;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private RuntimeService runtimeService;

    @Test
    void testHandleDropShipmentOrderConfirmed() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        doNothing().when(camundaHelper).correlateMessage(any(), any(), any());

        var message = DropshipmentPurchaseOrderBookedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .externalOrderNumber("13.2")
                .build();
        dropshipmentOrderService.handleDropShipmentOrderConfirmed(message);

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals("13.2", updatedOrder.getLatestJson().getOrderHeader().getOrderNumberExternal());
    }

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceived() {

        var salesOrder = createSalesOrder();
        var message = createShipmentConfirmedMessage(salesOrder);
        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(message);

        var optUpdatedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber());
        assertThat(optUpdatedSalesOrder).isNotEmpty();
        var updatedSalesOrder = optUpdatedSalesOrder.get();
        var updatedOrderRows = updatedSalesOrder.getLatestJson().getOrderRows();
        assertThat(updatedOrderRows).hasSize(3);

        var sku1Row = updatedOrderRows.get(0);
        var sku2Row = updatedOrderRows.get(1);
        var sku3Row = updatedOrderRows.get(2);

        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(sku1Row.getSku()).as("sku-1").isEqualTo("sku-1");
            softly.assertThat(sku1Row.getShippingProvider()).as("Service provider name").isEqualTo("abc1");
            softly.assertThat(sku1Row.getTrackingNumbers()).as("Size of tracking numbers sku-1").hasSize(1);
            softly.assertThat(sku1Row.getTrackingNumbers().get(0)).as("sku-1 tracking number").isEqualTo("00F8F0LT");

            softly.assertThat(sku2Row.getSku()).as("sku-2").isEqualTo("sku-2");
            softly.assertThat(sku2Row.getTrackingNumbers()).as("Size of tracking numbers sku-2").isNull();

            softly.assertThat(sku3Row.getSku()).as("sku-3").isEqualTo("sku-3");
            softly.assertThat(sku3Row.getShippingProvider()).as("Service provider name").isEqualTo("abc2");
            softly.assertThat(sku3Row.getTrackingNumbers()).as("Size of tracking numbers sku-3").hasSize(1);
            softly.assertThat(sku3Row.getTrackingNumbers().get(0)).as("sku-3 tracking number").isEqualTo("00F8F0LT2");
        }
        assertThat(updatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).hasSize(18);
        assertThat(updatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(LocalDateTime.now().getYear() + "-1000000000001");
    }

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceivedWhenThereIsAnotherInvoiceForSameYear() {

        var salesOrder = createSalesOrder();
        createSalesOrderInvoice(salesOrder);
        var message = createShipmentConfirmedMessage(salesOrder);
        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(message);

        var optUpdatedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber());
        assertThat(optUpdatedSalesOrder).isNotEmpty();
        var updatedSalesOrder = optUpdatedSalesOrder.get();

        assertThat(updatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).hasSize(18);
        assertThat(updatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(LocalDateTime.now().getYear() + "-1000000000002");
    }

    private void createSalesOrderInvoice(SalesOrder salesOrder) {
        var currentYear = LocalDateTime.now().getYear();
        var salesOrderInvoice = SalesOrderInvoice.builder()
                .source(InvoiceSource.SOH)
                .orderNumber(salesOrder.getOrderNumber())
                .salesOrder(salesOrder)
                .invoiceNumber(currentYear + "-1000000000001")
                .url("http://abc1")
                .build();
        invoiceService.saveInvoice(salesOrderInvoice);
    }

    private DropshipmentShipmentConfirmedMessage createShipmentConfirmedMessage(SalesOrder salesOrder) {
        var message = DropshipmentShipmentConfirmedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .items(Set.of(ShipmentItem.builder()
                        .productNumber("sku-1")
                        .parcelNumber("00F8F0LT")
                        .trackingLink("http://abc1")
                        .serviceProviderName("abc1")
                        .build(), ShipmentItem.builder()
                        .productNumber("sku-3")
                        .parcelNumber("00F8F0LT2")
                        .trackingLink("http://abc2")
                        .serviceProviderName("abc2")
                        .build()))
                .build();
        return message;
    }

    private SalesOrder createSalesOrder() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        doNothing().when(camundaHelper).correlateMessage(any(), any(), any());
        return salesOrder;
    }

    @Test
    void testHandleDropShipmentOrderRowCancellation() {
        var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        doNothing().when(camundaHelper).correlateMessage(any(), any(), any());
        dropshipmentOrderService.handleDropShipmentOrderRowCancellation(salesOrder.getOrderNumber(), "sku-1");
        dropshipmentOrderService.handleDropShipmentOrderRowCancellation(salesOrder.getOrderNumber(), "sku-2");
        dropshipmentOrderService.handleDropShipmentOrderRowCancellation(salesOrder.getOrderNumber(), "sku-3");

        verify(camundaHelper).correlateMessage(
                argThat(message -> message == DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED),
                argThat(order -> {
                    assertThat(order.getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
                    var orderRows = order.getLatestJson().getOrderRows();
                    assertThat(orderRows).hasSize(3);
                    return true;
                }),
                argThat(variableMap -> variableMap.getValue(ORDER_ROW_ID.getName(), String.class).equals("sku-1"))
        );

        verify(salesOrderService, times(3)).save(
                argThat(order -> StringUtils.equals(order.getOrderNumber(), salesOrder.getOrderNumber())),
                argThat(action -> action == Action.ORDER_ROW_CANCELLED)
        );

        verify(camundaHelper).correlateMessage(
                argThat(message -> message == DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED),
                argThat(order -> {
                    assertThat(order.getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
                    var orderRows = order.getLatestJson().getOrderRows();
                    assertThat(orderRows).hasSize(3);
                    return true;
                }),
                argThat(variableMap -> variableMap.getValue(ORDER_ROW_ID.getName(), String.class).equals("sku-2"))
        );

        verify(camundaHelper).correlateMessage(
                argThat(message -> message == DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED),
                argThat(order -> {
                    assertThat(order.getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
                    var orderRows = order.getLatestJson().getOrderRows();
                    assertThat(orderRows).hasSize(3);
                    return true;
                }),
                argThat(variableMap -> variableMap.getValue(ORDER_ROW_ID.getName(), String.class).equals("sku-3"))
        );

        var updatedSalesOrder =
                salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows())
                .isNotEmpty()
                .hasSize(3)
                .allMatch(orderRow -> Boolean.TRUE.equals(orderRow.getIsCancelled()));
    }

    @Test
    // Has to be removed and replaced with the corresponding model test (once model tests are integrated in soh)
    void testModelDropShipmentOrderRowsCancellation() {

        var salesOrder =
                SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER.getName())));

        var messageCorrelationResult = bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), false));

        assertThat(messageCorrelationResult.getExecution().getProcessInstanceId()).isNotBlank();

        salesOrder.getLatestJson().getOrderRows().forEach(orderRow -> {

            assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                    camundaHelper.hasPassed(processInstance.getId(), EVENT_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED.getName())));

            assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                    camundaHelper.hasPassed(processInstance.getId(), EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLED.getName())));
        });

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(processInstance.getId(), Events.END_MSG_ORDER_CANCELLED.getName())));

    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        auditLogRepository.deleteAll();
        salesOrderRepository.deleteAll();
        salesOrderInvoiceRepository.deleteAll();
    }
}