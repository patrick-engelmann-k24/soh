package de.kfzteile24.salesOrderHub.services.salesorder;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
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
    private BpmUtil bpmUtil;

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

        assertFalse(timedPollingService.pollWithDefaultTiming(() -> bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName())));

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

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName())));

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

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> bpmUtil.isProcessWaitingAtExpectedToken(orderProcessInstance, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName())));

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
