package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentOrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.EventType;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.StringUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Spy;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_ROW_CANCELLATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.assertSalesCreditNoteCreatedMessage;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
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
    @SpyBean
    private KeyValuePropertyService keyValuePropertyService;
    @SpyBean
    private PublishDropshipmentOrderCreatedDelegate publishDropshipmentOrderCreatedDelegate;

    @SpyBean
    private SalesOrderReturnService salesOrderReturnService;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

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
    @SneakyThrows
    void testHandleDropShipmentPurchaseOrderReturnConfirmed() {

        String nextCreditNoteNumber = salesOrderReturnService.createCreditNoteNumber();
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        doNothing().when(camundaHelper).correlateMessage(any(), any(), any());

        String rawMessage = readResource("examples/dropshipmentPurchaseOrderReturnConfirmed.json");
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderReturnConfirmedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message);

        SalesOrderReturn updatedOrder = salesOrderReturnService.getByOrderNumber(salesOrder.getOrderNumber() + "-" + nextCreditNoteNumber);
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage = updatedOrder.getSalesCreditNoteCreatedMessage();
        assertNotNull(updatedOrder);
        assertEquals(nextCreditNoteNumber, updatedOrder.getSalesCreditNoteCreatedMessage().getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber());

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo(salesOrder.getOrderNumber() + "-" + nextCreditNoteNumber);
        assertSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage, salesOrder);
    }

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceived() throws JsonProcessingException {

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
    void testHandleDropShipmentOrderTrackingInformationReceivedWhenThereIsAnotherInvoiceForSameYear() throws JsonProcessingException {

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

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceivedWhenTrackingNumberListIsNullAndMultipleParcelNumbersReceived() throws JsonProcessingException {

        var salesOrder = createSalesOrder();
        createSalesOrderInvoice(salesOrder);
        var message = createShipmentConfirmedMessage(salesOrder);
        message.getItems().add(ShipmentItem.builder()
                        .productNumber("sku-1")
                        .parcelNumber("00F8F0LT5")
                        .trackingLink("http://abc5")
                        .serviceProviderName("abc5")
                        .build());
        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(message);

        var optUpdatedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber());
        assertThat(optUpdatedSalesOrder).isNotEmpty();
        var updatedSalesOrder = optUpdatedSalesOrder.get();

        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(0).getTrackingNumbers()).hasSize(2);
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
                .items(new ArrayList<>(Set.of(ShipmentItem.builder()
                        .productNumber("sku-1")
                        .parcelNumber("00F8F0LT")
                        .trackingLink("http://abc1")
                        .serviceProviderName("abc1")
                        .build(), ShipmentItem.builder()
                        .productNumber("sku-3")
                        .parcelNumber("00F8F0LT2")
                        .trackingLink("http://abc2")
                        .serviceProviderName("abc2")
                        .build())))
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
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

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

    @Test
    void testModelContinueDropShipmentOrderProcessing() throws Exception {

        var salesOrders = List.of(
                createAndSaveDropshipmentOrder("111111111"),
                createAndSaveDropshipmentOrder("222222222"),
                createAndSaveDropshipmentOrder("333333333"));

        setPauseDropshipmentProcessingFlag(true);

        doNothing().when(publishDropshipmentOrderCreatedDelegate).execute(any());

        var processInstances = salesOrders.stream()
                .map(salesOrder -> camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP))
                .collect(Collectors.toUnmodifiableList());

        salesOrders.forEach(salesOrder -> assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber()))));

        processInstances.forEach(processInstance -> {
            assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                    camundaHelper.hasPassed(processInstance.getId(), XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName())));

            assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                    camundaHelper.hasNotPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

            bpmUtil.isProcessWaitingAtExpectedToken(processInstance, EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        });

        var processInstanceIds = processInstances.stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toUnmodifiableList());

        var waitingProcessInstancesBeforeSignal = getWaitingProcessInstanceIds();

        assertThat(waitingProcessInstancesBeforeSignal).containsAll(processInstanceIds);

        camundaHelper.sendSignal(Signals.CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS);

        var waitingProcessInstancesAfterSignal = getWaitingProcessInstanceIds();

        assertThat(processInstanceIds).isNotIn(waitingProcessInstancesAfterSignal);

        processInstances.forEach(processInstance -> {
            assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                    camundaHelper.hasPassed(processInstance.getId(), EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName())));

            assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(5), () ->
                    camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));
        });

    }

    @Test
    void testModelPauseDropshipmentOrderProcessingTrue() {

        var salesOrder = createAndSaveDropshipmentOrder("111111111");

        setPauseDropshipmentProcessingFlag(true);

        var processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        bpmUtil.isProcessWaitingAtExpectedToken(processInstance, EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasNotPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

    }

    @Test
    void testModelPauseDropshipmentOrderProcessingFalse() throws Exception {

        var salesOrder = createAndSaveDropshipmentOrder("111111111");

        var processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasNotPassed(processInstance.getId(), EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(5), () ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForSetPauseDropshipmentProcessing")
    void setPauseDropshipmentProcessing(Boolean currentPauseDropshipmentProcessing,
                                        Boolean newPauseDropshipmentProcessing,
                                        VerificationMode verificationMode) {

        setPauseDropshipmentProcessingFlag(currentPauseDropshipmentProcessing);
        doNothing().when(camundaHelper).sendSignal(any());

        var savedPauseDropshipmentProcessingProperty =
                dropshipmentOrderService.setPauseDropshipmentProcessing(newPauseDropshipmentProcessing);

        assertThat(savedPauseDropshipmentProcessingProperty.getKey()).isEqualTo(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING);
        assertThat(savedPauseDropshipmentProcessingProperty.getValue()).isEqualTo(newPauseDropshipmentProcessing.toString());

        verify(camundaHelper, verificationMode).sendSignal(Signals.CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS);

    }

    private static Stream<Arguments> provideArgumentsForSetPauseDropshipmentProcessing() {
        return Stream.of(
                Arguments.of(Boolean.FALSE, Boolean.FALSE, never()),
                Arguments.of(Boolean.FALSE, Boolean.TRUE, never()),
                Arguments.of(Boolean.TRUE, Boolean.TRUE, never()),
                Arguments.of(Boolean.TRUE, Boolean.FALSE, times(1))
        );
    }

    private List<String> getWaitingProcessInstanceIds() {
        return bpmUtil.findEventSubscriptions(EventType.SIGNAL,
                Signals.CONTINUE_PROCESSING_DROPSHIPMENT_ORDERS.getName()).stream()
                .map(EventSubscription::getProcessInstanceId)
                .collect(Collectors.toUnmodifiableList());
    }

    private void setPauseDropshipmentProcessingFlag(Boolean value) {
        keyValuePropertyService.getPropertyByKey(PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING)
                .ifPresentOrElse(property -> {
                    property.setValue(value.toString());
                    keyValuePropertyService.save(property);
                }, () -> {
                    throw new NotFoundException("Could not found persistent property. Key:  " + PersistentProperties.PAUSE_DROPSHIPMENT_PROCESSING);
                });
    }

    @Commit
    private SalesOrder createAndSaveDropshipmentOrder(String orderNumber) {
        var salesOrder =
                SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber(orderNumber);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        salesOrderService.save(salesOrder, Action.ORDER_CREATED);
        return salesOrder;
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
        setPauseDropshipmentProcessingFlag(false);
        auditLogRepository.deleteAll();
        salesOrderRepository.deleteAll();
        salesOrderInvoiceRepository.deleteAll();
    }
}