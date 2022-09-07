package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShipped;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ROW_CANCELLED;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

class SalesOrderRowServiceIntegrationTest extends AbstractIntegrationTest {

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
    private TimedPollingService timerService;

    @Autowired
    private CamundaHelper camundaHelper;

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
    public void testOrderRowCancelled() {
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

    @Test
    public void testCancelAllOrderRows() {
        final var salesOrder =
                salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = salesOrder.getOrderNumber();
        final var orderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());

        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRowSkus.get(0)) &&
                        camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRowSkus.get(1)) &&
                        camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRowSkus.get(2)) &&
                        camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())
        ));

        salesOrderRowService.cancelOrderRows(salesOrder.getOrderNumber(), orderRowSkus);

        final var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedSalesOrder.isPresent());

        Order latestJson = updatedSalesOrder.get().getLatestJson();
        for (OrderRows orderRows : latestJson.getOrderRows()) {
            assertTrue(orderRows.getIsCancelled());
            assertThatNoSubprocessExists(orderNumber, orderRows.getSku());
        }

        auditLogUtil.assertAuditLogExists(salesOrder.getId(), ORDER_ROW_CANCELLED, 3);
        assertFalse(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRowSkus.get(0)) &&
                        camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRowSkus.get(1)) &&
                        camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRowSkus.get(2)) &&
                        camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())
        ));
    }

    @Test
    void testHandleParcelShippedEvent() {
        final SalesOrder salesOrder1 = salesOrderUtil.createPersistedSalesOrder(LocalDateTime.now(), "sku1");
        final SalesOrder salesOrder2 = salesOrderUtil.createPersistedSalesOrder(
                salesOrder1.getOrderNumber(), LocalDateTime.now(), "sku1", "sku2", "sku3");
        salesOrderUtil.createPersistedSalesOrder(salesOrder1.getOrderNumber(), LocalDateTime.now(), "sku4", "sku5");
        var orderNumber = salesOrder1.getOrderNumber();
        var event = ParcelShipped.builder()
                .orderNumber(orderNumber)
                .deliveryNoteNumber("delivery-note-12345")
                .trackingNumber("tracking-12345")
                .trackingLink("http://tacking-link")
                .logisticsPartnerName("dhl")
                .articleItemsDtos(Collections.singleton(
                        ArticleItemsDto.builder()
                                .number("sku1")
                                .quantity(BigDecimal.ONE)
                                .description("sku name 1")
                                .isDeposit(false)
                                .build()
                ))
                .build();

        salesOrderRowService.handleParcelShippedEvent(event);

        final var expectedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder2.getOrderNumber());
        assertTrue(expectedSalesOrder.isPresent());

        verify(snsPublishService).publishSalesOrderShipmentConfirmedEvent(
                argThat(
                        order -> {
                            assertThat(order.getOrderNumber()).isEqualTo(expectedSalesOrder.get().getOrderNumber());
                            assertThat(order.getOrderGroupId()).isEqualTo(expectedSalesOrder.get().getOrderGroupId());
                            return true;
                        }),
                argThat(
                        trackingLinks -> {
                            assertTrue(trackingLinks.stream().findFirst().isPresent());
                            assertThat(trackingLinks.stream().findFirst().get().getUrl()).isEqualTo(event.getTrackingLink());
                            var list = event.getArticleItemsDtos().stream().map(ArticleItemsDto::getNumber).collect(toList());
                            assertThat(trackingLinks.stream().findFirst().get().getOrderItems()).isEqualTo(list);
                            return true;
                        })
        );

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
        assertFalse(timerService.poll(Duration.ofSeconds(7), Duration.ofSeconds(2), () ->
                camundaHelper.checkIfOrderRowProcessExists(orderNumber, sku)
        ));
    }
}
