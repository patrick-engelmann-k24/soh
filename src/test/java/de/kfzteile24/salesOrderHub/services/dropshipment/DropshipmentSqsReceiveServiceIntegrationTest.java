package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderRow;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author stefand
 */

@Slf4j
class DropshipmentSqsReceiveServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

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

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

        bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Map.of(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true));

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName())));

        dropshipmentSqsReceiveService.queueListenerDropshipmentShipmentConfirmed(getDropshipmentShipmentConfirmed(salesOrder), ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
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

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        String message = readResource("examples/dropshipmentOrderPurchasedBooked.json");
        message = message.replace("123", salesOrder.getOrderNumber());
        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName())));

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));
    }

    @Test
    void testQueueListenerDropshipmentOrderPurchasedBookedFalse() {

        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        order.getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        String message = readResource("examples/dropshipmentOrderPurchasedBookedFalse.json");
        message = message.replace("123", salesOrder.getOrderNumber());
        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(message, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName())));

        assertTrue(timedPollingService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName())));
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
