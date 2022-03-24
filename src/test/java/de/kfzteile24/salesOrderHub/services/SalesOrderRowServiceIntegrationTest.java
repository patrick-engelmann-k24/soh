package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
import java.util.List;

import org.assertj.core.util.Lists;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.history.HistoricProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class SalesOrderRowServiceIntegrationTest {

    @Autowired
    private SalesOrderRowService salesOrderRowService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

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
    private ProcessEngine processEngine;

    @BeforeEach
    public void setup() {
        reset();
        init(processEngine);
    }

    @Test
    @SuppressWarnings("unchecked")
    void orderRowsCanBeCancelledBeforePaymentSecuredHasBeenReceived() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var skuToCancel = orderRowSkus.get(1);

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        assertThatNoSubprocessExists(orderNumber, skuToCancel);

        salesOrderRowService.cancelOrderRows(salesOrder.getOrderNumber(), Lists.newArrayList(skuToCancel));

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedSalesOrder.isPresent());

        Order latestJson = updatedSalesOrder.get().getLatestJson();
        assertThat(latestJson.getOrderRows().stream().filter(OrderRows::getIsCancelled).count()).isZero();
        checkTotalsValues(latestJson.getOrderHeader().getTotals());

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();

        var updatedSkus = (List<String>) runtimeService.getVariable(historicProcessInstance.getId(),
                Variables.ORDER_ROWS.getName());
        assertThat(updatedSkus.size()).isEqualTo(orderRowSkus.size() - 1);
        assertThat(updatedSkus.stream().anyMatch(sku -> sku.equals(skuToCancel))).isFalse();

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_ROW_CANCELLED);
        assertThatNoSubprocessExists(orderNumber, skuToCancel);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void orderRowCancelled() {
        final var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var skuToCancel = orderRowSkus.get(1);

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        salesOrderRowService.cancelOrderRows(salesOrder.getOrderNumber(), Lists.newArrayList(skuToCancel));

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedSalesOrder.isPresent());

        Order latestJson = updatedSalesOrder.get().getLatestJson();
        assertThat(latestJson.getOrderRows().stream().filter(OrderRows::getIsCancelled).count()).isZero();
        checkTotalsValues(latestJson.getOrderHeader().getTotals());

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(SALES_ORDER_PROCESS.getName())
                .variableValueEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .singleResult();

        var updatedSkus = (List<String>) runtimeService.getVariable(historicProcessInstance.getId(),
                Variables.ORDER_ROWS.getName());
        assertThat(updatedSkus.size()).isEqualTo(orderRowSkus.size() - 1);
        assertThat(updatedSkus.stream().anyMatch(sku -> sku.equals(skuToCancel))).isFalse();

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_ROW_CANCELLED);
        assertThatNoSubprocessExists(orderNumber, skuToCancel);
    }

    private void checkTotalsValues(Totals totals) {

        assertEquals(BigDecimal.valueOf(91), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.valueOf(77), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(87), totals.getTotalDiscountGross());
        assertEquals(BigDecimal.valueOf(69), totals.getTotalDiscountNet());
        assertEquals(BigDecimal.valueOf(4), totals.getGrandTotalGross());
        assertEquals(BigDecimal.valueOf(4), totals.getPaymentTotal());
        assertEquals(BigDecimal.valueOf(8), totals.getGrandTotalNet());
    }

    public void assertThatNoSubprocessExists(String orderNumber, String sku) {
        assertThatThrownBy(() ->
                runtimeService.createMessageCorrelation(Messages.ORDER_CANCELLATION_RECEIVED.getName())
                        .processInstanceBusinessKey(orderNumber)
                        .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), sku)
                        .correlateWithResult())
                .isInstanceOf(MismatchingMessageCorrelationException.class);
    }

    @Test
    @Transactional
    public void multipleOrderRowCancellationCanBeDoneAccordingToSameOrderGroupId() {

        // create first sales order
        final var salesOrder1 = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderRowSkus = salesOrder1.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var skuToCancel = orderRowSkus.get(1);
        final var aliveSkus = orderRowSkus.stream().filter(sku -> !sku.equals(skuToCancel)).collect(Collectors.toList());

        // create second sales order with same values except order number
        final var salesOrder2 = salesOrderUtil.createPersistedSalesOrderV3WithDiffGroupId(false, REGULAR, CREDIT_CARD, NEW, salesOrder1.getOrderGroupId());

        try {
            camundaHelper.createOrderProcess(salesOrder1, Messages.ORDER_RECEIVED_ECP);
            assertThat(compareIsCancelledFields(salesOrder1, orderRowSkus)).isTrue();
            assertThat(compareIsCancelledFields(salesOrder2, orderRowSkus)).isTrue();
            salesOrderRowService.cancelOrderRows(salesOrder1.getOrderNumber(), Lists.newArrayList(skuToCancel));


            assertThat(compareIsCancelledFields(salesOrder1, aliveSkus)).isTrue();
            assertThat(compareIsCancelledFields(salesOrder2, aliveSkus)).isTrue();
        } finally {
            assert salesOrder1.getId() != null;
            assert salesOrder2.getId() != null;
            deleteTestInput(List.of(salesOrder1.getId(), salesOrder2.getId()));
        }
    }

    private boolean compareIsCancelledFields(SalesOrder salesOrder, List<String> skuList) {


        assert salesOrder.getId() != null;
        var foundSalesOrder = salesOrderRepository.findById(salesOrder.getId());
        if (foundSalesOrder.isPresent()) {
            AtomicBoolean result = new AtomicBoolean(true);
            return foundSalesOrder.get().getLatestJson().getOrderRows().stream()
                    .map(OrderRows::getSku)
                    .collect(Collectors.toList())
                    .containsAll(skuList);

        } else {
            throw new SalesOrderNotFoundException(salesOrder.getOrderNumber());
        }
    }

    private void deleteTestInput(List<UUID> orderIdList) {
        orderIdList.forEach(uuid -> salesOrderRepository.deleteById(uuid));
    }
}
