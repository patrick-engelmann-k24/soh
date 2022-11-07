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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
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
    private CamundaHelper camundaHelper;

    @Autowired
    private SalesOrderCancelledService salesOrderCancelledService;


    @Test
    public void testHandleCoreSalesOrderCancelled() {
        var message = CoreSalesOrderCancelledMessage.builder().build();
        var messageWrapper = MessageWrapper.builder().build();
        final var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var orderNumber = salesOrder.getOrderNumber();
        message.setOrderNumber(orderNumber);

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber).get();
        assertTrue(updatedSalesOrder.isCancelled());

        Order latestJson = updatedSalesOrder.getLatestJson();
        for (OrderRows orderRows : latestJson.getOrderRows()) {
            assertFalse(orderRows.getIsCancelled());
        }

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_CANCELLED, 1);
    }
}
