package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
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
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_GROUP_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
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

    @Autowired
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
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private ObjectUtil objectUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @SpyBean
    private SnsPublishService snsPublishService;
    @SpyBean
    private SalesOrderRowService salesOrderRowService;

    @BeforeEach
    public void setup() {
        reset();
        init(processEngine);
        bpmUtil.cleanUp();
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
                .variableValueEquals(ORDER_GROUP_ID.getName(), salesOrder.getOrderGroupId())
                .variableValueEquals(ORDER_ROW_ID.getName(), orderRowId)
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
                .variableValueEquals(ORDER_GROUP_ID.getName(), salesOrder1.getOrderGroupId())
                .variableValueEquals(ORDER_ROW_ID.getName(), orderRowId)
                .list();

        assertThat(processInstanceList1).hasSize(1);

        var processInstanceList2 = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                .variableValueEquals(ORDER_NUMBER.getName(), salesOrder2.getOrderNumber())
                .variableValueEquals(ORDER_GROUP_ID.getName(), salesOrder1.getOrderGroupId())
                .variableValueEquals(ORDER_ROW_ID.getName(), orderRowId)
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

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber("580309129");

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        var dropshipmentShipmentConfirmed = readResource("examples/dropshipmentShipmentConfirmed.json");
        sqsReceiveService.queueListenerDropshipmentShipmentConfirmed(dropshipmentShipmentConfirmed, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

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

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(eq(updatedSalesOrder), argThat(
                trackingNumbers -> {
                    assertThat(trackingNumbers).hasSize(2);
                    assertThat(trackingNumbers).containsExactlyInAnyOrder("http://abc1", "http://abc2");
                    return true;
                }
        ));
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBooked() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        bpmUtil.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        String message = readResource("examples/dropshipmentOrderPurchasedBooked.json");
        message = message.replace("123", salesOrder.getOrderNumber());
        sqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals("13.2", updatedOrder.getLatestJson().getOrderHeader().getOrderNumberExternal());
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBookedFalse() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        bpmUtil.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        assertTrue(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())) &&
                camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13012") &&
                camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13013")
        );

        String message = readResource("examples/dropshipmentOrderPurchasedBookedFalse.json");
        message =message.replace("123",salesOrder.getOrderNumber());
            sqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message,ANY_SENDER_ID,ANY_RECEIVE_COUNT);

        assertFalse(timerService.poll(Duration.ofSeconds(7),Duration.

        ofSeconds(2), ()->
                camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(),"2270-13012")&&
                camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(),"2270-13013")&&
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())
                ));

        SalesOrder updated = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);

        assertNotNull(updated);

        assertEquals("13.2",updated.getLatestJson().getOrderHeader().getOrderNumberExternal());
    }

    @SneakyThrows
    @Test
    @DisplayName("IT core sales credit note created event handling")
    void testQueueListenerCoreSalesCreditNoteCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderGroupId("580309129");
        salesOrder.setOrderNumber("580309129");

        var orderRows = List.of(
                OrderRows.builder()
                        .sumValues(SumValues.builder()
                                .goodsValueGross(BigDecimal.valueOf(3.38))
                                .goodsValueNet(BigDecimal.valueOf(2))
                                .build())
                        .unitValues(UnitValues.builder()
                                .goodsValueGross(BigDecimal.valueOf(3.38))
                                .goodsValueNet(BigDecimal.valueOf(2))
                                .build())
                        .taxRate(BigDecimal.valueOf(19))
                        .quantity(BigDecimal.valueOf(2))
                        .sku("sku-1")
                        .build()
        );

        var totals = Totals.builder()
                .goodsTotalGross(BigDecimal.valueOf(2.38))
                .goodsTotalNet(BigDecimal.valueOf(2))
                .grandTotalGross(BigDecimal.valueOf(2.38))
                .grandTotalNet(BigDecimal.valueOf(2))
                .paymentTotal(BigDecimal.valueOf(2.38))
                .grandTotalTaxes(List.of(GrandTotalTaxes.builder()
                        .rate(BigDecimal.valueOf(19))
                        .value(BigDecimal.valueOf(0.38))
                        .build()))
                .build();

        salesOrder.getLatestJson().getOrderHeader().setTotals(totals);
        salesOrder.getLatestJson().setOrderRows(orderRows);

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        var coreReturnDeliveryNotePrinted =  readResource("examples/coreSalesCreditNoteCreated.json");
        String body = objectMapper.readValue(coreReturnDeliveryNotePrinted, SqsMessage.class).getBody();
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                objectMapper.readValue(body, SalesCreditNoteCreatedMessage.class);
        sqsReceiveService.queueListenerCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        //set expected values
        var expectedSalesOrder = createSalesOrderFromOrder(salesOrder);
        expectedSalesOrder.setVersion(1L);
        var order = expectedSalesOrder.getLatestJson();
        var latestOrderJson = objectUtil.deepCopyOf(order, Order.class);
        latestOrderJson.getOrderHeader().getTotals().setShippingCostGross(BigDecimal.valueOf(11.90).setScale(2, RoundingMode.HALF_UP));
        latestOrderJson.getOrderHeader().getTotals().setShippingCostNet(BigDecimal.valueOf(10.00).setScale(2, RoundingMode.HALF_UP));
        expectedSalesOrder.setLatestJson(latestOrderJson);

        verify(salesOrderService).findLastOrderByOrderGroupId("580309129");

        verify(salesOrderRowService).handleSalesOrderReturn(eq("580309129"), eq(salesCreditNoteCreatedMessage));

        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo("876130");
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo("580309129");
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
    public void cleanup() {
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        bpmUtil.cleanUp();
    }
}
