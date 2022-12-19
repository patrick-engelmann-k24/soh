package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
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

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private BpmUtil bpmUtil;

    @BeforeEach
    public void setup() {
        super.setUp();
        bpmUtil.cleanUp();
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
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

        SalesOrder salesOrder2 = createSalesOrderWithRandomOrderNumber();
        callQueueListenerDropshipmentShipmentConfirmed(salesOrder2);
        var savedSalesOrder2 = salesOrderRepository.getOrderByOrderNumber(salesOrder2.getOrderNumber());
        assertTrue(savedSalesOrder2.isPresent());

        SalesOrder salesOrder3 = createSalesOrderWithRandomOrderNumber();
        callQueueListenerDropshipmentShipmentConfirmed(salesOrder3);

        var savedSalesOrder3 = salesOrderRepository.getOrderByOrderNumber(salesOrder3.getOrderNumber());
        assertTrue(savedSalesOrder3.isPresent());
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
        for (SalesOrder order : orders) {
            var savedSalesOrder = salesOrderRepository.getOrderByOrderNumber(order.getOrderNumber());
            assertTrue(savedSalesOrder.isPresent());
        }
    }

    private SalesOrder createSalesOrderWithRandomOrderNumber() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        String randomOrderNumber = bpmUtil.getRandomOrderNumber();
        salesOrder.setOrderNumber(randomOrderNumber);
        salesOrder.getLatestJson().getOrderHeader().setOrderNumber(randomOrderNumber);
        salesOrderService.save(salesOrder, Action.ORDER_CREATED);
        return salesOrder;
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

        assertTrue(timedPollingService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(orderProcess.getProcessInstanceId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));

        dropshipmentSqsReceiveService.queueListenerDropshipmentShipmentConfirmed(getDropshipmentShipmentConfirmed(salesOrder), messageWrapper);


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

        assertTrue(timedPollingService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(salesOrder.getProcessId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

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

        dropshipmentOrderService.setPreventDropshipmentOrderReturnConfirmed(true);
        assertThatThrownBy(() -> dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining(MessageFormat.format("Dropshipment Order Return Confirmed process is inactive. Message with Order number {0} is moved to DLQ", salesOrder.getOrderNumber()));

        dropshipmentOrderService.setPreventDropshipmentOrderReturnConfirmed(false);
        dropshipmentSqsReceiveService.queueListenerDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper);

        verify(salesOrderReturnService).handleSalesOrderReturn(
                argThat(msg -> StringUtils.equals(msg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber(), salesOrder.getOrderNumber())),
                eq(DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED),
                eq(DROPSHIPMENT_ORDER_RETURN_CONFIRMED)
        );
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> bpmUtil.cleanUp());
    }
}
