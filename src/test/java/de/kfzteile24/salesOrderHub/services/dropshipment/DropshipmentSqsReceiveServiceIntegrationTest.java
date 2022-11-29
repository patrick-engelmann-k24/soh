package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
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
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderRow;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@Slf4j
class DropshipmentSqsReceiveServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentSqsReceiveService dropshipmentSqsReceiveService;
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

    private final MessageWrapper messageWrapper = MessageWrapper.builder().build();

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
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        String randomOrderNumber = bpmUtil.getRandomOrderNumber();
        message.getOrderHeader().setOrderNumber(randomOrderNumber);
        message.getOrderHeader().setOrderGroupId(randomOrderNumber);
        message.getOrderHeader().setOrderNumberCore(RandomStringUtils.randomNumeric(9));
        var orderRows = List.of(
                createOrderRow("sku-1", NONE),
                createOrderRow("sku-2", NONE),
                createOrderRow("sku-3", NONE)
        );
        message.setOrderRows(orderRows);
        message.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        return salesOrderService.createSalesOrder(createSalesOrderFromOrder(message));
    }

    @SneakyThrows
    private void callQueueListenerDropshipmentShipmentConfirmed(SalesOrder salesOrder) {
        ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(orderProcess.getProcessInstanceId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

        bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Map.of(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true));

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(orderProcess.getProcessInstanceId(), THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName())));

        dropshipmentSqsReceiveService.queueListenerDropshipmentShipmentConfirmed(getDropshipmentShipmentConfirmed(salesOrder), messageWrapper);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(orderProcess.getProcessInstanceId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(orderProcess.getProcessInstanceId(), EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName())));
    }

    @NotNull
    private DropshipmentShipmentConfirmedMessage getDropshipmentShipmentConfirmed(SalesOrder salesOrder) {
        var message = getObjectByResource("dropshipmentShipmentConfirmed.json", DropshipmentShipmentConfirmedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());
        return message;
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBooked() {

        var orderMessage = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        orderMessage.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var message = getObjectByResource("dropshipmentOrderPurchasedBooked.json", DropshipmentPurchaseOrderBookedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());
        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, messageWrapper);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName())));

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBookedFalse() {

        var orderMessage = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        orderMessage.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        var message = getObjectByResource("dropshipmentOrderPurchasedBooked.json", DropshipmentPurchaseOrderBookedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());
        message.setBooked(false);
        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, messageWrapper);

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName())));

        assertTrue(timedPollingService.pollWithDefaultTiming(() -> 
                camundaHelper.hasPassed(salesOrder.getProcessId(), DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName())));
    }

    @Test
    void testQueueListenerDropshipmentOrderReturnConfirmed() {

        var orderMessage = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        orderMessage.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(orderMessage));

        var message = getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json",
                DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        message.setSalesOrderNumber(salesOrder.getOrderNumber());
        assertThatThrownBy(() -> dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining(MessageFormat.format("Dropshipment Order Return Confirmed process is inactive. Message with Order number {0} is moved to DLQ", salesOrder.getOrderNumber()));

        dropshipmentOrderService.setPreventDropshipmentOrderReturnConfirmed(false);
        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper);

        verify(dropshipmentOrderService).handleDropshipmentPurchaseOrderReturnConfirmed(
                argThat(msg -> StringUtils.equals(msg.getSalesOrderNumber(), salesOrder.getOrderNumber())),
                eq(messageWrapper)
        );
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
