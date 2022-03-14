package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationItem;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
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
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class
)
public class SalesOrderRowServiceIntegrationTest {

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
    public void orderRowCancelledBeforePaymentSecuredHasBeenReceived() {
        final var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var skuToCancel = orderRowSkus.get(1);

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        assertThatNoSubprocessExists(orderNumber, skuToCancel);

        CoreCancellationMessage coreCancellationMessage = getCoreCancellationMessage(salesOrder, skuToCancel);
        salesOrderRowService.cancelOrderRows(coreCancellationMessage);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedSalesOrder.isPresent());

        Order latestJson = updatedSalesOrder.get().getLatestJson();
        for (OrderRows orderRows : latestJson.getOrderRows()) {
            if (orderRows.getSku().equals(skuToCancel)) {
                assertTrue(orderRows.getIsCancelled());
                checkTotalsValues(latestJson.getOrderHeader().getTotals());
            } else {
                assertFalse(orderRows.getIsCancelled());
            }
        }

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

        CoreCancellationMessage coreCancellationMessage = getCoreCancellationMessage(salesOrder, skuToCancel);
        salesOrderRowService.cancelOrderRows(coreCancellationMessage);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedSalesOrder.isPresent());

        Order latestJson = updatedSalesOrder.get().getLatestJson();
        for (OrderRows orderRows : latestJson.getOrderRows()) {
            if (orderRows.getSku().equals(skuToCancel)) {
                assertTrue(orderRows.getIsCancelled());
                checkTotalsValues(latestJson.getOrderHeader().getTotals());
            } else {
                assertFalse(orderRows.getIsCancelled());
            }
        }

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

    private CoreCancellationMessage getCoreCancellationMessage(SalesOrder salesOrder, String skuToCancel) {
        List<CoreCancellationItem> items = salesOrder.getLatestJson().getOrderRows()
                .stream().filter(orderRows -> orderRows.getSku().equals(skuToCancel))
                .map(orderRows ->
                        CoreCancellationItem.builder()
                                .sku(orderRows.getSku())
                                .quantity(orderRows.getQuantity().intValue()).build()).collect(toList());

        return CoreCancellationMessage.builder()
                .orderNumber(salesOrder.getOrderNumber())
                .items(items)
                .build();
    }

    private void checkTotalsValues(Totals totals) {

        assertEquals(BigDecimal.valueOf(90), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.valueOf(79), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(90), totals.getTotalDiscountGross());
        assertEquals(BigDecimal.valueOf(79), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(0), totals.getGrandTotalGross());
        assertEquals(BigDecimal.valueOf(0), totals.getPaymentTotal());
        assertEquals(BigDecimal.valueOf(0), totals.getGrandTotalNet());
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
    @SuppressWarnings("unchecked")
    public void multipleOrderRowCancellationCanBeDoneAccordingToSameOrderGroupId() {

        // create first sales order
        final var salesOrder1 = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderRowSkus = salesOrder1.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());
        final var skuToCancel = orderRowSkus.get(1);

        // create second sales order with same values except order number
        final var salesOrder2 = salesOrderUtil.createPersistedSalesOrderV3WithDiffGroupId(false, REGULAR, CREDIT_CARD, NEW, salesOrder1.getOrderGroupId());

        try {
            camundaHelper.createOrderProcess(salesOrder1, Messages.ORDER_RECEIVED_ECP);
            CoreCancellationMessage coreCancellationMessage = getCoreCancellationMessage(salesOrder1, skuToCancel);

            assertThat(compareIsCancelledFields(salesOrder1, List.of(false, false, false))).isTrue();
            assertThat(compareIsCancelledFields(salesOrder2, List.of(false, false, false))).isTrue();
            salesOrderRowService.cancelOrderRows(coreCancellationMessage);


            assertThat(compareIsCancelledFields(salesOrder1, List.of(false, true, false))).isTrue();
            assertThat(compareIsCancelledFields(salesOrder2, List.of(false, true, false))).isTrue();
        } finally {
            assert salesOrder1.getId() != null;
            assert salesOrder2.getId() != null;
            deleteTestInput(List.of(salesOrder1.getId(), salesOrder2.getId()));
        }
    }

    private boolean compareIsCancelledFields(SalesOrder salesOrder, List<Boolean> isCancelledList) {


        assert salesOrder.getId() != null;
        var foundSalesOrder = salesOrderRepository.findById(salesOrder.getId());
        if (foundSalesOrder.isPresent()) {
            AtomicBoolean result = new AtomicBoolean(true);
            List<OrderRows> orderRows = foundSalesOrder.get().getLatestJson().getOrderRows();
            IntStream.range(0, orderRows.size())
                    .forEach(idx ->
                            result.set(result.get() && isCancelledList.get(idx).equals(orderRows.get(idx).getIsCancelled()))
                    );
            return result.get();

        } else {
            throw new SalesOrderNotFoundException(salesOrder.getOrderNumber());
        }
    }

    private void deleteTestInput(List<UUID> orderIdList) {
        orderIdList.forEach(uuid -> salesOrderRepository.deleteById(uuid));
    }
}
