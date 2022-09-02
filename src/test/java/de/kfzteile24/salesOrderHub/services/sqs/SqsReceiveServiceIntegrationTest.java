package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.InvoiceNumberCounterService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderRow;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author stefand
 */

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Slf4j
class SqsReceiveServiceIntegrationTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @SpyBean
    private CamundaHelper camundaHelper;
    @Autowired
    private SqsReceiveService sqsReceiveService;
    @Autowired
    private TimedPollingService timerService;
    @SpyBean
    private SalesOrderService salesOrderService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvoiceNumberCounterRepository invoiceNumberCounterRepository;
    @Autowired
    private InvoiceNumberCounterService invoiceNumberCounterService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private SalesOrderReturnService salesOrderReturnService;
    @Autowired
    private ObjectMapper objectMapper;
    @SpyBean
    private SnsPublishService snsPublishService;
    @SpyBean
    private SalesOrderRowService salesOrderRowService;
    @Autowired
    private TimedPollingService timedPollingService;

    @BeforeEach
    public void setup() {
        reset();
        init(processEngine);
        bpmUtil.cleanUp();
        invoiceNumberCounterRepository.deleteAll();
        invoiceNumberCounterService.init();
    }

    @Test
    public void testQueueListenerItemTrackingIdReceived() {

        var senderId = "Ecp";
        var salesOrder = salesOrderUtil.createNewSalesOrder();
        var orderRowId = salesOrder.getLatestJson().getOrderRows().get(0).getSku();

        createOrderRowProcessWaitingOnTransmittedToLogistics(salesOrder);

        String fulfillmentMessage = getFulfillmentMsg(salesOrder, orderRowId);
        sqsReceiveService.queueListenerOrderItemTransmittedToLogistic(fulfillmentMessage, senderId, 1);

        var processInstanceList = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                .variableValueEquals(ORDER_NUMBER.getName(), salesOrder.getOrderNumber())
                .list();

        assertThat(processInstanceList).hasSize(1);
    }

    private String getFulfillmentMsg(SalesOrder salesOrder, String orderRowId) {
        String fulfillmentMessage = readResource("examples/fulfillmentMessage.json");
        fulfillmentMessage = fulfillmentMessage.replace("524001240", salesOrder.getOrderNumber());
        fulfillmentMessage = fulfillmentMessage.replace("1130-0713", orderRowId);
        return fulfillmentMessage;
    }

    @Test
    public void testQueueListenerItemTrackingIdReceivedIfMultipleOrderExistsForSameGroupId() {

        var senderId = "Ecp";
        var salesOrder1 = salesOrderUtil.createNewSalesOrder();
        var orderRowId = salesOrder1.getLatestJson().getOrderRows().get(0).getSku();
        var salesOrder2 = salesOrderUtil.createNewSalesOrderWithCustomSkusAndGroupId(
                salesOrder1.getOrderGroupId(),
                orderRowId,
                bpmUtil.getRandomOrderNumber());

        createOrderRowProcessWaitingOnTransmittedToLogistics(salesOrder1);
        createOrderRowProcessWaitingOnTransmittedToLogistics(salesOrder2);

        String fulfillmentMessage = getFulfillmentMsg(salesOrder1, orderRowId);
        sqsReceiveService.queueListenerOrderItemTransmittedToLogistic(fulfillmentMessage, senderId, 1);

        var processInstanceList1 = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                .variableValueEquals(ORDER_NUMBER.getName(), salesOrder1.getOrderNumber())
                .list();

        assertThat(processInstanceList1).hasSize(1);

        var processInstanceList2 = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                .variableValueEquals(ORDER_NUMBER.getName(), salesOrder2.getOrderNumber())
                .list();

        assertThat(processInstanceList2).hasSize(1);
    }

    private void createOrderRowProcessWaitingOnTransmittedToLogistics(SalesOrder salesOrder) {
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(ORDER_NUMBER.getName(), salesOrder.getOrderNumber());
        processVariables.put(SHIPMENT_METHOD.getName(), REGULAR.getName());
        var orderRowId = salesOrder.getLatestJson().getOrderRows().get(0).getSku();

        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        bpmUtil.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                salesOrder.getOrderNumber() + "#" + orderRowId,
                processVariables);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassed(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName()
        );

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasNotPassed(
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName()
        );
    }

    @Test
    void testQueueListenerOrderPaymentSecuredWithPaypalPayment() {

        String orderRawMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderNumber("500000996");
        order.getOrderHeader().getPayments().get(0).setType("paypal");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        String coreDataReaderEvent = readResource("examples/coreDataReaderEvent.json");
        sqsReceiveService.queueListenerOrderPaymentSecured(coreDataReaderEvent, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        assertFalse(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13012")));

    }

    @Test
    void testQueueListenerOrderPaymentSecuredWithCreditcardPayment() {

        String orderRawMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderNumber("500000996");
        order.getOrderHeader().getPayments().get(0).setType("creditcard");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        String coreDataReaderEvent = readResource("examples/coreDataReaderEvent.json");
        sqsReceiveService.queueListenerOrderPaymentSecured(coreDataReaderEvent, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertFalse(isWaitingForPaymentSecured);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "1130-0713")));

    }

    @Test
    void testQueueListenerD365OrderPaymentSecured() {

        String orderRawMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderNumber("500000996");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));

        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        String orderPaymentSecuredMessage = readResource("examples/d365OrderPaymentSecuredMessageWithOneOrderNumber.json");
        sqsReceiveService.queueListenerD365OrderPaymentSecured(orderPaymentSecuredMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertFalse(isWaitingForPaymentSecured);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "1130-0713")));

    }

    @Test
    @DisplayName("IT testing dropshipment shipment confirmed event handling")
    void testQueueListenerDropshipmentShipmentConfirmed(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        SalesOrder salesOrder = createSalesOrderWithRandomOrderNumber();
        callQueueListenerDropshipmentShipmentConfirmed(salesOrder);

        var savedSalesOrder = salesOrderRepository.getOrderByOrderNumber(salesOrder.getOrderNumber());
        LocalDateTime now = LocalDateTime.now();
        assertTrue(savedSalesOrder.isPresent());
        assertEquals(now.getYear() + "-1000000000001", savedSalesOrder.get().getLatestJson().getOrderHeader().getDocumentRefNumber());
        assertEquals(now.getYear() + "-1000000000001", savedSalesOrder.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
    }

    @Test
    @DisplayName("IT testing dropshipment shipment confirmed event handling multiple time to see the increment on invoice number")
    void testQueueListenerDropshipmentShipmentConfirmedMultipleTime(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        SalesOrder salesOrder1 = createSalesOrderWithRandomOrderNumber();
            callQueueListenerDropshipmentShipmentConfirmed(salesOrder1);

        var savedSalesOrder1 = salesOrderRepository.getOrderByOrderNumber(salesOrder1.getOrderNumber());
        LocalDateTime now = LocalDateTime.now();
        assertTrue(savedSalesOrder1.isPresent());
        assertEquals(now.getYear() + "-1000000000001", savedSalesOrder1.get().getLatestJson().getOrderHeader().getDocumentRefNumber());
        assertEquals(now.getYear() + "-1000000000001", savedSalesOrder1.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());

        SalesOrder salesOrder2 = createSalesOrderWithRandomOrderNumber();
        callQueueListenerDropshipmentShipmentConfirmed(salesOrder2);
        var savedSalesOrder2 = salesOrderRepository.getOrderByOrderNumber(salesOrder2.getOrderNumber());
        assertTrue(savedSalesOrder2.isPresent());
        assertEquals(now.getYear() + "-1000000000002", savedSalesOrder2.get().getLatestJson().getOrderHeader().getDocumentRefNumber());
        assertEquals(now.getYear() + "-1000000000002", savedSalesOrder2.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());

        SalesOrder salesOrder3 = createSalesOrderWithRandomOrderNumber();
        callQueueListenerDropshipmentShipmentConfirmed(salesOrder3);

        var savedSalesOrder3 = salesOrderRepository.getOrderByOrderNumber(salesOrder3.getOrderNumber());
        assertTrue(savedSalesOrder3.isPresent());
        assertEquals(now.getYear() + "-1000000000003", savedSalesOrder3.get().getLatestJson().getOrderHeader().getDocumentRefNumber());
        assertEquals(now.getYear() + "-1000000000003", savedSalesOrder3.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
    }

    @Test
    @DisplayName("IT testing dropshipment shipment confirmed event handling multiple time in threads to see the increment on invoice number")
    void testQueueListenerDropshipmentShipmentConfirmedMultipleTimeInThreads(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());
        int numberOfOrders = 10;
        List<SalesOrder> orders = new ArrayList<>();
        for (int i = 0; i < numberOfOrders; i++) {
            orders.add(createSalesOrderWithRandomOrderNumber());
        }

        long start = Instant.now().toEpochMilli();
        for (SalesOrder order : orders) {

            Thread thread = new Thread(() -> callQueueListenerDropshipmentShipmentConfirmed(order));

            thread.start();
        }
        System.out.println("Test Info: Multithread calls are finished in : " + (Instant.now().toEpochMilli() - start) + " milliseconds");
        SalesOrder salesOrder11 = createSalesOrderWithRandomOrderNumber();

        long start2 = Instant.now().toEpochMilli();
        callQueueListenerDropshipmentShipmentConfirmed(salesOrder11);
        System.out.println("Test Info: One dropshipment method time is : " + (Instant.now().toEpochMilli() - start2) + " milliseconds");

        orders.add(salesOrder11);
        var invoiceNumbers = new TreeSet<Long>();
        for (SalesOrder order : orders) {
            var savedSalesOrder = salesOrderRepository.getOrderByOrderNumber(order.getOrderNumber());
            assertTrue(savedSalesOrder.isPresent());
            invoiceNumbers.add(Long.valueOf(savedSalesOrder.get().getLatestJson().getOrderHeader().getDocumentRefNumber().substring(6)));
        }
        int counter = 1;
        for (Long number : invoiceNumbers) {
            assertEquals(counter, number);
            counter++;
        }
    }

    private SalesOrder createSalesOrderWithRandomOrderNumber() {
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        String randomOrderNumber = bpmUtil.getRandomOrderNumber();
        order.getOrderHeader().setOrderNumber(randomOrderNumber);
        order.getOrderHeader().setOrderGroupId(randomOrderNumber);
        order.getOrderHeader().setOrderNumberCore(RandomStringUtils.randomNumeric(9));
        var orderRows = List.of(
                createOrderRow("sku-1", NONE),
                createOrderRow("sku-2", NONE),
                createOrderRow("sku-3", NONE)
        );
        order.setOrderRows(orderRows);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        return salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
    }

    private void callQueueListenerDropshipmentShipmentConfirmed(SalesOrder salesOrder) {
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

        bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Map.of(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName())));

        sqsReceiveService.queueListenerDropshipmentShipmentConfirmed(getDropshipmentShipmentConfirmed(salesOrder), ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName())));
    }

    @NotNull
    private String getDropshipmentShipmentConfirmed(SalesOrder salesOrder) {
        var dropshipmentShipmentConfirmed = readResource("examples/dropshipmentShipmentConfirmed.json");
        dropshipmentShipmentConfirmed = dropshipmentShipmentConfirmed.replace("580309129", salesOrder.getOrderNumber());
        return dropshipmentShipmentConfirmed;
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBooked() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        String message = readResource("examples/dropshipmentOrderPurchasedBooked.json");
        message = message.replace("123", salesOrder.getOrderNumber());
        sqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBookedFalse() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        String message = readResource("examples/dropshipmentOrderPurchasedBookedFalse.json");
        message = message.replace("123", salesOrder.getOrderNumber());
        sqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName())));

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName())));
    }

    @SneakyThrows
    @Test
    @DisplayName("IT core sales credit note created event handling")
    void testQueueListenerCoreSalesCreditNoteCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderGroupId("580309129");
        salesOrder.setOrderNumber("580309129");
        salesOrder.getLatestJson().getOrderHeader().getTotals().setGrandTotalTaxes(List.of(GrandTotalTaxes.builder()
                .rate(BigDecimal.valueOf(19))
                .value(BigDecimal.valueOf(0.38))
                .build()));

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");
        String body = objectMapper.readValue(coreReturnDeliveryNotePrinted, SqsMessage.class).getBody();
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                objectMapper.readValue(body, SalesCreditNoteCreatedMessage.class);
        sqsReceiveService.queueListenerCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        SalesCreditNoteHeader salesCreditNoteHeader = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader();
        String returnOrderNumber = createOrderNumberInSOH(salesCreditNoteHeader.getOrderNumber(), salesCreditNoteHeader.getCreditNoteNumber());
        SalesOrderReturn returnSalesOrder = salesOrderReturnService.getByOrderNumber(returnOrderNumber);
        Order returnOrder = returnSalesOrder.getReturnOrderJson();

        checkOrderRowValues(returnOrder.getOrderRows());
        checkTotalsValues(returnOrder.getOrderHeader().getTotals());
        checkEventIsPublished(salesCreditNoteCreatedMessage);
    }

    private void checkOrderRowValues(List<OrderRows> returnOrderRows) {

        assertEquals(3, returnOrderRows.size());

        OrderRows orderRows = returnOrderRows.stream().filter(r -> r.getSku().equals("sku-1")).findFirst().orElse(null);
        checkOrderRowValues(orderRows, "name-sku-1",
                new BigDecimal("1"), new BigDecimal("19.0"),
                new BigDecimal("-1.0"), new BigDecimal("-1.19"),
                new BigDecimal("-1.0"), new BigDecimal("-1.19"));

        orderRows = returnOrderRows.stream().filter(r -> r.getSku().equals("new-sku")).findFirst().orElse(null);
        checkOrderRowValues(orderRows, "new-sku",
                new BigDecimal("1"), new BigDecimal("19.0"),
                new BigDecimal("10.0"), new BigDecimal("11.90"),
                new BigDecimal("10.0"), new BigDecimal("11.90"));

        orderRows = returnOrderRows.stream().filter(r -> r.getSku().equals("sku-2")).findFirst().orElse(null);
        checkOrderRowValues(orderRows, "name-sku-2",
                new BigDecimal("-2"), new BigDecimal("20.0"),
                new BigDecimal("17.39"), new BigDecimal("20.87"),
                new BigDecimal("-34.78"), new BigDecimal("-41.74"));
    }

    private void checkOrderRowValues(OrderRows orderRows, String itemDescription,
                                     BigDecimal quantity, BigDecimal taxRate,
                                     BigDecimal unitNetValue, BigDecimal unitGrossValue,
                                     BigDecimal sumNetValue, BigDecimal sumGrossValue) {

        assertNotNull(orderRows);
        assertEquals(itemDescription, orderRows.getName());
        assertEquals(quantity, orderRows.getQuantity());
        assertEquals(taxRate, orderRows.getTaxRate());
        assertEquals("shipment_regular", orderRows.getShippingType());

        assertEquals(unitNetValue, orderRows.getUnitValues().getGoodsValueNet());
        assertEquals(unitGrossValue, orderRows.getUnitValues().getGoodsValueGross());

        assertEquals(sumNetValue, orderRows.getSumValues().getGoodsValueNet());
        assertEquals(sumGrossValue, orderRows.getSumValues().getGoodsValueGross());
    }

    private void checkTotalsValues(Totals totals) {

        assertNotNull(totals);

        assertEquals(new BigDecimal("-25.78"), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal("-31.03"), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.ZERO, totals.getTotalDiscountNet());
        assertEquals(BigDecimal.ZERO, totals.getTotalDiscountGross());
        assertEquals(new BigDecimal("-35.78"), totals.getGrandTotalNet());
        assertEquals(new BigDecimal("-42.93"), totals.getGrandTotalGross());
        assertEquals(new BigDecimal("-42.93"), totals.getPaymentTotal());
        assertEquals(new BigDecimal("-10.0"), totals.getShippingCostNet());
        assertEquals(new BigDecimal("-11.90"), totals.getShippingCostGross());

        List<GrandTotalTaxes> grandTotalTaxes = totals.getGrandTotalTaxes();
        assertEquals(2, grandTotalTaxes.size());
        GrandTotalTaxes grandTotalTax = grandTotalTaxes.stream().filter(tax -> tax.getRate().equals(new BigDecimal("19.0"))).findFirst().orElse(null);
        assertNotNull(grandTotalTax);
        assertEquals(new BigDecimal("-0.19"), grandTotalTax.getValue());
        grandTotalTax = grandTotalTaxes.stream().filter(tax -> tax.getRate().equals(new BigDecimal("20.0"))).findFirst().orElse(null);
        assertNotNull(grandTotalTax);
        assertEquals(new BigDecimal("-6.96"), grandTotalTax.getValue());
    }

    private void checkEventIsPublished(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage) {

        verify(salesOrderService).getOrderByOrderNumber("580309129");
        verify(salesOrderRowService).handleSalesOrderReturn(eq(salesCreditNoteCreatedMessage), eq(RETURN_ORDER_CREATED), eq(CORE_CREDIT_NOTE_CREATED));
        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo("580309129-876130");
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo("580309129");
                    return true;
                }
        ));
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreated() throws URISyntaxException, IOException {

        URI uri = Objects.requireNonNull(getClass().getClassLoader().getResource("examples/product/DZN.json")).toURI();
        byte[] bytes = Files.readAllBytes(Paths.get(uri));
        WireMockServer wireMockServer = new WireMockServer(18080);
        wireMockServer.start();
        wireMockServer.stubFor(WireMock.post(urlEqualTo("/oauth2/token"))
                .willReturn(aResponse().withBody("{ \"access_token\": \"fake_access_token\" }")));
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/json/v15/?sku=1130-0713"))
                .willReturn(aResponse().withBody(bytes)));

        String orderRawMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(orderRawMessage);

        sqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(orderRawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(7),
                () -> camundaHelper.checkIfActiveProcessExists(order.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        assertEquals(order, updated.getLatestJson());
        assertEquals(order, updated.getOriginalOrder());

        wireMockServer.stop();
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreatedDuplicateOrder() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber()))
        );

        sqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(orderRawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timerService.pollWithDefaultTiming(
                () -> camundaHelper.checkIfActiveProcessExists(order.getOrderHeader().getOrderNumber())));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).orElse(null);
        assertNotNull(updated);
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        assertEquals(order, updated.getLatestJson());
        assertEquals(order, updated.getOriginalOrder());
    }

    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreated() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        String migrationInvoiceMsg = readResource("examples/coreSalesInvoiceCreatedMultipleItems.json");

        //Replace order number with randomly created order number as expected
        migrationInvoiceMsg = migrationInvoiceMsg.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerMigrationCoreSalesInvoiceCreated(migrationInvoiceMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
    }

    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreatedDuplicateSubsequentOrder() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku = "2010-10183";
        String invoiceMsg = readResource("examples/coreSalesInvoiceCreatedOneItem.json");

        //Replace order number with randomly created order number as expected
        invoiceMsg = invoiceMsg.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));

        sqsReceiveService.queueListenerMigrationCoreSalesInvoiceCreated(invoiceMsg, "Migration Delivery", 1);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));

        verify(snsPublishService).publishMigrationOrderRowCancelled(eq(originalOrderNumber), eq(rowSku));
        verify(snsPublishService).publishMigrationOrderCreated(eq(newOrderNumberCreatedInSoh));
    }

    @Test
    @DisplayName("IT migration core sales credit note created event handling")
    void testQueueListenerMigrationCoreSalesCreditNoteCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = salesOrderUtil.createSalesOrderForMigrationInvoiceTest();
        var orderNumber = salesOrder.getOrderNumber();
        var creditNumber = "876130";

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");
        sqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createOrderNumberInSOH(orderNumber, creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
    }

    @Test
    @DisplayName("IT migration core sales credit note created event handling if related return order already exists")
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedDuplicateReturnOrder(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = salesOrderUtil.createSalesOrderForMigrationInvoiceTest();
        var orderNumber = salesOrder.getOrderNumber();
        var creditNumber = "876130";

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");
        sqsReceiveService.queueListenerCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        sqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(argThat(
                (SalesOrderReturn salesOrderReturn) -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createOrderNumberInSOH(orderNumber, creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
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
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> bpmUtil.cleanUp());
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        invoiceNumberCounterService.init();
    }
}
