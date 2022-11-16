package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_ORDER_RECEIVED_FROM_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_ROW_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.TRACKING_LINKS;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreDropshipmentInvoiceDelegateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderInvoiceRepository salesOrderInvoiceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private TimedPollingService pollingService;

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private BpmUtil bpmUtil;

    @Autowired
    private OrderUtil orderUtil;

    @Test
    void testInvoiceIsStoredCorrectly() {
        var testOrder = salesOrderUtil.createNewDropshipmentSalesOrder();
        final var orderNumber = testOrder.getOrderNumber();
        var invoiceEvent = salesOrderUtil.createInvoiceCreatedMsg(orderNumber);
        invoiceEvent.getSalesInvoice().getSalesInvoiceHeader().getInvoiceLines().add(orderUtil.createShippingCostLineFromSalesOrder(testOrder));
        ((Order) testOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        salesOrderService.updateOrder(testOrder);

        ProcessInstance orderProcess = createAndVerifyOrderProcess(testOrder);
        sendAndVerifyDropshipmentOrderConfirmed(orderNumber, orderProcess);
        sendAndVerifyTrackingInfoReceived(orderNumber, orderProcess);
        verifyIfInvoiceEventIsSaved(orderNumber, invoiceEvent);
    }

    private ProcessInstance createAndVerifyOrderProcess(SalesOrder testOrder) {
        var orderProcess = camundaHelper.createOrderProcess(testOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(orderProcess).hasPassedInOrder(
                    START_MSG_ORDER_RECEIVED_FROM_ECP.getName(),
                    XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                    EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName()
            );
            return true;
        }));
        return orderProcess;
    }

    private void sendAndVerifyDropshipmentOrderConfirmed(String orderNumber, ProcessInstance orderProcess) {
        bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED,
                orderNumber,
                Map.of(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true));
        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(orderProcess).hasPassedInOrder(
                    EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName(),
                    XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName(),
                    EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName()
            );
            return true;
        }));
    }

    private void sendAndVerifyTrackingInfoReceived(String orderNumber, ProcessInstance orderProcess) {
        util.sendMessage(DROPSHIPMENT_ORDER_ROW_TRACKING_INFORMATION_RECEIVED, orderNumber,
                Map.of(TRACKING_LINKS.getName(),
                        List.of(
                                "{\"url\":\"http://abc1\", \"order_items\":[\"1440-47378\"]}",
                                "{\"url\":\"http://abc2\", \"order_items\":[\"2010-10183\"]}"))
        );
        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(orderProcess).hasPassedInOrder(
                    EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName(),
                    "eventThrowMsgPublishInvoiceData"
            );
            return true;
        }));
    }

    private void verifyIfInvoiceEventIsSaved(String orderNumber, CoreSalesInvoiceCreatedMessage invoiceEvent) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        invoiceEvent.getSalesInvoice().getSalesInvoiceHeader().setInvoiceNumber(null);
        invoiceEvent.getSalesInvoice().getSalesInvoiceHeader().setInvoiceDate(
                salesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceDate());
        org.assertj.core.api.Assertions.assertThat(salesOrder.getInvoiceEvent()).isEqualTo(invoiceEvent);
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> salesOrderInvoiceRepository.deleteAll());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> auditLogRepository.deleteAll());
    }
}
