package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
import org.assertj.core.util.Lists;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class SalesOrderCancelledServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SalesOrderRowService salesOrderRowService;

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
        var message = getObjectByResource("coreSalesOrderCancelledMessage.json",  CoreSalesOrderCancelledMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        final var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var orderNumber = salesOrder.getOrderNumber();
        message.setOrderNumber(orderNumber);
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber).get();
        assertTrue(updatedSalesOrder.isCancelled());

        Order latestJson = updatedSalesOrder.getLatestJson();
        for (OrderRows orderRows : latestJson.getOrderRows()) {
            assertTrue(orderRows.getIsCancelled());
        }

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_ROW_CANCELLED, 3);
    }
}
