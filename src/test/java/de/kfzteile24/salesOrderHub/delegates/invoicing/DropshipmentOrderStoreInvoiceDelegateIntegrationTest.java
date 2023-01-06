package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.PERSIST_DROPSHIPMENT_ORDER_ITEMS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_ORDER_RECEIVED_FROM_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropshipmentOrderStoreInvoiceDelegateIntegrationTest extends AbstractIntegrationTest {

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
    private BpmUtil bpmUtil;

    @Autowired
    private OrderUtil orderUtil;

    @Autowired
    private SalesOrderProcessService salesOrderProcessService;

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
    }

    private ProcessInstance createAndVerifyOrderProcess(SalesOrder testOrder) {
        var orderProcess = salesOrderProcessService.createOrderProcess(testOrder, Messages.ORDER_RECEIVED_ECP);
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
                    THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName(),
                    XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName(),
                    PERSIST_DROPSHIPMENT_ORDER_ITEMS.getName()
            );
            assertThat(orderProcess).isWaitingAtExactly(MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED.getName());
            return true;
        }));
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> salesOrderInvoiceRepository.deleteAll());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> auditLogRepository.deleteAll());
    }
}
