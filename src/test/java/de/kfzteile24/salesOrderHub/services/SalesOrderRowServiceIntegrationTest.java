package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class
)
public class SalesOrderRowServiceIntegrationTest {

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
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private TimedPollingService pollingService;

    @Autowired
    private ProcessEngine processEngine;

    @BeforeEach
    public void setup() {
        init(processEngine);
    }

    @Test
    public void orderRowsCanBeCancelledBeforePaymentSecuredHasBeenReceived() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var skuToCancel = orderRowSkus.get(1);

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertThat(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()))
                .isTrue();
        assertThatNoSubprocessExists(orderNumber, skuToCancel);

        salesOrderRowService.cancelOrderRowWithoutSubprocess(orderNumber, skuToCancel);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        updatedSalesOrder.get().getLatestJson().getOrderRows().forEach(orderRow ->
        assertThat(orderRow.getIsCancelled()).isEqualTo(orderRow.getSku().equals(skuToCancel)));

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();

        var updatedSkus = (List<String>) runtimeService.getVariable(historicProcessInstance.getId(),
                Variables.ORDER_ROWS.getName());
        assertThat(updatedSkus.size()).isEqualTo(orderRowSkus.size() - 1);
        assertThat(updatedSkus.stream().anyMatch(sku -> sku.equals(skuToCancel))).isFalse();

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_ROW_CANCELLED);

        assertThat(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()))
                .isTrue();
    }

    @Test
    public void cancellingAllOrderRowsBeforePaymentSecuredHasBeenReceivedCancelsTheWholeOrder() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        final var processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertThat(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()))
                .isTrue();
        assertThatNoSubprocessExists(salesOrder.getOrderNumber(),
                salesOrder.getLatestJson().getOrderRows().get(0).getSku());

        salesOrder.getLatestJson().getOrderRows().forEach(orderRow ->
            salesOrderRowService.cancelOrderRowWithoutSubprocess(salesOrder.getOrderNumber(), orderRow.getSku()));

        final var hasProcessEnded = pollingService.pollWithDefaultTiming(() -> {
            BpmnAwareTests.assertThat(processInstance).isEnded();
            return true;
        });
        assertThat(hasProcessEnded).isTrue();


        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_CANCELLED);
    }

    public void assertThatNoSubprocessExists(String orderNumber, String sku) {
        assertThatThrownBy(() ->
                runtimeService.createMessageCorrelation(Messages.ORDER_CANCELLATION_RECEIVED.getName())
                        .processInstanceBusinessKey(orderNumber)
                        .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), sku)
                        .correlateWithResult())
                .isInstanceOf(MismatchingMessageCorrelationException.class);
    }
}
