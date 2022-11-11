package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.SUB_PROCESS_CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CANCELLED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesOrderCancelledServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditLogUtil auditLogUtil;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private TimedPollingService timerService;

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private SalesOrderCancelledService salesOrderCancelledService;

    @Autowired
    private TimedPollingService pollingService;


    @Test
    public void testHandleCoreSalesOrderCancelled() {
        var message = CoreSalesOrderCancelledMessage.builder().build();
        var messageWrapper = MessageWrapper.builder().build();
        final var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_ORDER_CREATED.getName())));

        var orderNumber = salesOrder.getOrderNumber();
        message.setOrderNumber(orderNumber);

        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);

        final var correctActivityOrder = pollingService.poll(Duration.ofSeconds(10), Duration.ofSeconds(2), () -> {
            BpmnAwareTests.assertThat(processInstance).hasPassedInOrder(
                    START_MSG_CORE_SALES_ORDER_CANCELLED.getName(),
                    CORE_SALES_ORDER_CANCELLED.getName(),
                    END_MSG_CORE_SALES_ORDER_CANCELLED.getName(),
                    SUB_PROCESS_CORE_SALES_ORDER_CANCELLED.getName()
            );
            return true;
        });
        assertTrue(correctActivityOrder);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber).get();
        assertTrue(updatedSalesOrder.isCancelled());

        Order latestJson = updatedSalesOrder.getLatestJson();
        for (OrderRows orderRows : latestJson.getOrderRows()) {
            assertFalse(orderRows.getIsCancelled());
        }

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_CANCELLED, 1);
    }
}
