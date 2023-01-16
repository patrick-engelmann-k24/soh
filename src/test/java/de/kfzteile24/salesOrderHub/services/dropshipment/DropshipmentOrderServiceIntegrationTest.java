package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.EventType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Signals;
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
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.ORDER_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.RETURN_ORDER_NUMBER_PREFIX;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.CALL_ACTIVITY_DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.PERSIST_DROPSHIPMENT_ORDER_ITEMS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.assertSalesCreditNoteCreatedMessage;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DropshipmentOrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentOrderService dropshipmentOrderService;
    @Autowired
    private DropshipmentShipmentService dropshipmentShipmentService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvoiceService invoiceService;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    protected DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Autowired
    protected DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;
    @Autowired
    private SalesOrderProcessService salesOrderProcessService;

    @BeforeEach
    public void setup() {
        super.setUp();
        timedPollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
        timedPollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAllInBatch());
    }

    @Test
    void testHandleDropShipmentOrderConfirmed() {
        Order order = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        doNothing().when(camundaHelper).correlateMessage(any(), any(SalesOrder.class), any());

        var message = DropshipmentPurchaseOrderBookedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .externalOrderNumber("13.2")
                .booked(true)
                .build();
        dropshipmentOrderService.handleDropShipmentOrderConfirmed(message, messageWrapper);

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals("13.2", updatedOrder.getLatestJson().getOrderHeader().getOrderNumberExternal());
    }

    @Test
    @SneakyThrows
    void testHandleDropShipmentPurchaseOrderReturnConfirmed() {

        String nextCreditNoteNumber = createNextCreditNoteNumberCounter(salesOrderReturnService.createCreditNoteNumber());
        Order order = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        doReturn(messageCorrelationResult).when(camundaHelper).correlateMessage(any(), anyString(), any());
        DropshipmentPurchaseOrderReturnConfirmedMessage message =
                getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json",  DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());

        dropshipmentOrderService.setPreventDropshipmentOrderReturnConfirmed(false);
        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper);

        String returnOrderNumber = RETURN_ORDER_NUMBER_PREFIX + ORDER_NUMBER_SEPARATOR + nextCreditNoteNumber;
        SalesOrderReturn updatedOrder = salesOrderReturnService.getByOrderNumber(returnOrderNumber)
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(returnOrderNumber));
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage = updatedOrder.getSalesCreditNoteCreatedMessage();
        assertNotNull(updatedOrder);
        assertEquals(nextCreditNoteNumber, updatedOrder.getSalesCreditNoteCreatedMessage().getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber());

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo(returnOrderNumber);
        assertSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage, salesOrder);
    }

    private String createNextCreditNoteNumberCounter(String creditNoteNumber) {
        if (creditNoteNumber.length() != 10) {
            throw new IllegalArgumentException("Credit note number does not contain 10 digits");
        }
        var counter = Long.valueOf(creditNoteNumber.substring(5));
        counter++;
        return creditNoteNumber.substring(0, 5) + String.format("%05d", counter);
    }

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceived() {

        var salesOrder = createDropshipmentSalesOrder();

        startDropshipmentConfirmedProcess(salesOrder, true);

        var message = createShipmentConfirmedMessage(salesOrder);
        assertThat(dropshipmentInvoiceRowService.getByOrderNumber(salesOrder.getOrderNumber()).size()).isEqualTo(0);
        dropshipmentShipmentService.handleDropshipmentShipmentConfirmed(message, messageWrapper);

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

        assertThat(dropshipmentInvoiceRowService.getByOrderNumber(salesOrder.getOrderNumber()).size()).isEqualTo(2);

        var dropshipmentInvoiceRow1 = dropshipmentInvoiceRowService.
                getBySkuAndOrderNumber(sku1Row.getSku(), salesOrder.getOrderNumber()).orElseThrow();
        assertThat(dropshipmentInvoiceRow1.getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
        assertThat(dropshipmentInvoiceRow1.getSku()).isEqualTo(sku1Row.getSku());

        var dropshipmentInvoiceRow2 = dropshipmentInvoiceRowService.
                getBySkuAndOrderNumber(sku2Row.getSku(), salesOrder.getOrderNumber());
        assertThat(dropshipmentInvoiceRow2).isNotPresent();

        var dropshipmentInvoiceRow3 = dropshipmentInvoiceRowService.
                getBySkuAndOrderNumber(sku3Row.getSku(), salesOrder.getOrderNumber()).orElseThrow();
        assertThat(dropshipmentInvoiceRow3.getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
        assertThat(dropshipmentInvoiceRow3.getSku()).isEqualTo(sku3Row.getSku());
    }

    @Test
    void testHandleDropShipmentOrderTrackingInformationReceivedWhenThereIsAnotherInvoiceForSameYear() {

        var salesOrder1 = createDropshipmentSalesOrder();
        createSalesOrderInvoice(salesOrder1);
        var salesOrder2 = createDropshipmentSalesOrder();
        createSalesOrderInvoice(salesOrder2);

        startDropshipmentConfirmedProcess(salesOrder1, true);
        startDropshipmentConfirmedProcess(salesOrder2, true);

        dropshipmentShipmentService.handleDropshipmentShipmentConfirmed(createShipmentConfirmedMessage(salesOrder1), messageWrapper);
        dropshipmentShipmentService.handleDropshipmentShipmentConfirmed(createShipmentConfirmedMessage(salesOrder2), messageWrapper);

        var optUpdatedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder2.getOrderNumber());
        assertThat(optUpdatedSalesOrder).isNotEmpty();
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
        return DropshipmentShipmentConfirmedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .purchaseOrderNumber(salesOrder.getOrderNumber())
                .supplierInternalId(10)
                .shipmentDate(RandomStringUtils.randomAlphabetic(10))
                .items(new ArrayList<>(Set.of(ShipmentItem.builder()
                        .productNumber("sku-1")
                        .quantity(1)
                        .parcelNumber("00F8F0LT")
                        .trackingLink("http://abc1")
                        .serviceProviderName("abc1")
                        .build(), ShipmentItem.builder()
                        .productNumber("sku-3")
                        .parcelNumber("00F8F0LT2")
                        .quantity(2)
                        .trackingLink("http://abc2")
                        .serviceProviderName("abc2")
                        .build())))
                .build();
    }

    private SalesOrder createDropshipmentSalesOrder() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        salesOrder.setProcessId(salesOrder.getOrderNumber());

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        doNothing().when(camundaHelper).correlateMessage(any(), any(SalesOrder.class), any());
        return salesOrder;
    }

    @Test
    void testHandleDropShipmentOrderRowCancellation() {
        var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        doReturn(messageCorrelationResult).when(camundaHelper).correlateMessage(any(), anyString(), any());
        dropshipmentOrderService.handleDropShipmentOrderRowCancellation(salesOrder.getOrderNumber(), "sku-1");
        dropshipmentOrderService.handleDropShipmentOrderRowCancellation(salesOrder.getOrderNumber(), "sku-2");
        dropshipmentOrderService.handleDropShipmentOrderRowCancellation(salesOrder.getOrderNumber(), "sku-3");

        verify(salesOrderService, times(3)).save(
                argThat(order -> StringUtils.equals(order.getOrderNumber(), salesOrder.getOrderNumber())),
                argThat(action -> action == Action.ORDER_ROW_CANCELLED)
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

        var salesOrder = createDropshipmentSalesOrder();

        final var processInstance = startDropshipmentConfirmedProcess(salesOrder, false);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), Events.END_MSG_ORDER_CANCELLED.getName())));

        var updatedSalesOrder =
                salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        updatedSalesOrder.getLatestJson().getOrderRows().forEach(orderRow -> assertThat(orderRow.getIsCancelled()).isTrue());
        assertThat(updatedSalesOrder.isCancelled()).isTrue();
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
                .map(salesOrder -> salesOrderProcessService.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP))
                .collect(Collectors.toUnmodifiableList());

        salesOrders.forEach(salesOrder -> assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, salesOrder.getOrderNumber()))));

        processInstances.forEach(processInstance -> {
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName())));

            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasNotPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

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
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName())));

            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));
        });

    }

    @Test
    void testModelPauseDropshipmentOrderProcessingTrue() {

        var salesOrder = createAndSaveDropshipmentOrder("111111111");

        setPauseDropshipmentProcessingFlag(true);

        var processInstance = salesOrderProcessService.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, salesOrder.getOrderNumber())));

        bpmUtil.isProcessWaitingAtExpectedToken(processInstance, EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasNotPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

    }

    @Test
    void testModelPauseDropshipmentOrderProcessingFalse() {

        var salesOrder = createAndSaveDropshipmentOrder("111111111");

        var processInstance = salesOrderProcessService.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(SALES_ORDER_PROCESS, salesOrder.getOrderNumber())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasNotPassed(processInstance.getId(), EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

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

    private ProcessInstance startDropshipmentConfirmedProcess(SalesOrder salesOrder, boolean dropshipmentConfirmed) {
        ProcessInstance processInstance = salesOrderProcessService.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertThat(processInstance.getProcessInstanceId()).isNotNull();

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

        var messageCorrelationResult = bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), dropshipmentConfirmed));

        assertThat(messageCorrelationResult.getExecution().getProcessInstanceId()).isNotBlank();

        if (dropshipmentConfirmed) {
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), PERSIST_DROPSHIPMENT_ORDER_ITEMS.getName())));
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.isProcessWaitingAtExpectedToken(processInstance, MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED.getName())));
        } else {
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName())));
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName())));
            assertTrue(timerService.pollWithDefaultTiming(() ->
                    bpmUtil.hasPassed(processInstance.getId(), CALL_ACTIVITY_DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName())));
        }

        return processInstance;
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

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        setPauseDropshipmentProcessingFlag(false);
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
    }
}