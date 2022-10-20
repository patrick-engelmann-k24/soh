package de.kfzteile24.salesOrderHub.services.salesorder;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class SalesOrderSqsReceiveServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SalesOrderSqsReceiveService salesOrderSqsReceiveService;
    @Autowired
    private TimedPollingService timedPollingService;
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
    private SalesOrderUtil salesOrderUtil;

    @BeforeEach
    public void setup() {
        super.setUp();
        bpmUtil.cleanUp();
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        invoiceNumberCounterRepository.deleteAll();
        invoiceNumberCounterService.init();
    }

    @Test
    public void testQueueListenerItemTrackingIdReceived() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        var orderRowId = salesOrder.getLatestJson().getOrderRows().get(0).getSku();

        createOrderRowProcessWaitingOnTransmittedToLogistics(salesOrder);

        var message = getFulfillmentMsg(salesOrder, orderRowId);
        var messageWrapper = MessageWrapper.builder().build();
        salesOrderSqsReceiveService.queueListenerOrderItemTransmittedToLogistic(message, messageWrapper);

        var processInstanceList = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName())
                .variableValueEquals(ORDER_NUMBER.getName(), salesOrder.getOrderNumber())
                .list();

        assertThat(processInstanceList).hasSize(1);
    }

    private FulfillmentMessage getFulfillmentMsg(SalesOrder salesOrder, String orderRowId) {
        var message = getObjectByResource("fulfillmentMessage.json", FulfillmentMessage.class);
        message.setOrderNumber(salesOrder.getOrderNumber());
        message.setOrderItemSku(orderRowId);
        return message;
    }

    @Test
    public void testQueueListenerItemTrackingIdReceivedIfMultipleOrderExistsForSameGroupId() {

        var salesOrder1 = salesOrderUtil.createNewSalesOrder();
        var orderRowId = salesOrder1.getLatestJson().getOrderRows().get(0).getSku();
        var salesOrder2 = salesOrderUtil.createNewSalesOrderWithCustomSkusAndGroupId(
                salesOrder1.getOrderGroupId(),
                orderRowId,
                bpmUtil.getRandomOrderNumber());

        createOrderRowProcessWaitingOnTransmittedToLogistics(salesOrder1);
        createOrderRowProcessWaitingOnTransmittedToLogistics(salesOrder2);

        var message = getFulfillmentMsg(salesOrder1, orderRowId);
        var messageWrapper = MessageWrapper.builder().build();
        salesOrderSqsReceiveService.queueListenerOrderItemTransmittedToLogistic(message, messageWrapper);

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

        var orderMessage = getObjectByResource("ecpOrderMessage.json", Order.class);
        orderMessage.getOrderHeader().setOrderNumber("500000996");
        orderMessage.getOrderHeader().getPayments().get(0).setType("paypal");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));

        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        var message = getObjectByResource("coreDataReaderEvent.json", CoreDataReaderEvent.class);
        salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message);

        isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        assertFalse(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13012")));

    }

    @Test
    void testQueueListenerOrderPaymentSecuredWithCreditcardPayment() {

        var orderMessage = getObjectByResource("ecpOrderMessage.json", Order.class);
        orderMessage.getOrderHeader().setOrderNumber("500000996");
        orderMessage.getOrderHeader().getPayments().get(0).setType("creditcard");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));

        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        var message = getObjectByResource("coreDataReaderEvent.json", CoreDataReaderEvent.class);
        salesOrderSqsReceiveService.queueListenerOrderPaymentSecured(message);

        isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertFalse(isWaitingForPaymentSecured);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "1130-0713")));

    }

    @Test
    void testQueueListenerD365OrderPaymentSecured() {

        var orderMessage = getObjectByResource("ecpOrderMessage.json", Order.class);
        orderMessage.getOrderHeader().setOrderNumber("500000996");
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));
        var messageWrapper = MessageWrapper.builder().build();

        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);

        var message = getObjectByResource("d365OrderPaymentSecuredMessageWithOneOrderNumber.json", OrderPaymentSecuredMessage.class);
        salesOrderSqsReceiveService.queueListenerD365OrderPaymentSecured(message, messageWrapper);

        isWaitingForPaymentSecured =
                bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertFalse(isWaitingForPaymentSecured);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "1130-0713")));

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
