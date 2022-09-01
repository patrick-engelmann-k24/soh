package de.kfzteile24.salesOrderHub.services;

import com.newrelic.api.agent.Insights;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.sqs.SqsReceiveService;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.ORDER_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_GROUP_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author samet
 */

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class SalesOrderCreatedInSohIntegrationTest {

    @Autowired
    private CamundaHelper camundaHelper;
    @Autowired
    private SqsReceiveService sqsReceiveService;
    @Autowired
    private SalesOrderService salesOrderService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private BpmUtil util;
    @MockBean
    private Insights insights;
    @Autowired
    private ObjectUtil objectUtil;

    @BeforeEach
    public void setup() {
        init(processEngine);
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreated() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku = "2010-10183";
        String invoiceEvent = readResource("examples/coreSalesInvoiceCreatedOneItem.json");

        //Replace order number with randomly created order number as expected
        invoiceEvent = invoiceEvent.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceEvent, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "12.95",
                "10.79",
                "12.95",
                "10.79",
                "12.95",
                "0",
                "0");

        verifyThatNewRelicIsCalled(newOrderNumberCreatedInSoh);
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreateWithMultipleItemsSameSkus() {

        var senderId = "Delivery";
        var receiveCount = 1;

        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceEvent = readResource("examples/coreSalesInvoiceCreatedMultipleItemsSameSkus.json");

        //Replace order number with randomly created order number as expected
        invoiceEvent = invoiceEvent.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceEvent, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
        assertTrue(timerService.pollWithDefaultTiming(() -> checkIfInvoiceCreatedReceivedProcessExists(newOrderNumberCreatedInSoh)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "834.52",
                "695.64",
                "847.60",
                "706.63",
                "847.60",
                "13.08",
                "10.99");

        verifyThatNewRelicIsCalled(newOrderNumberCreatedInSoh);
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWithMultipleItems() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        String invoiceEvent = readResource("examples/coreSalesInvoiceCreatedMultipleItems.json");

        //Replace order number with randomly created order number as expected
        invoiceEvent = invoiceEvent.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceEvent, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
        assertTrue(timerService.pollWithDefaultTiming(() -> checkIfInvoiceCreatedReceivedProcessExists(newOrderNumberCreatedInSoh)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "445.60",
                "371.63",
                "445.60",
                "13.08",
                "10.99");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWithNotConsolidatedDuplicateItems() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();
        String originalOrderNumber = salesOrder.getOrderNumber();
        OrderRows duplicatedOrderRow = objectUtil.deepCopyOf(salesOrder.getLatestJson().getOrderRows().get(0), OrderRows.class);
        salesOrder.getLatestJson().getOrderRows().get(0).setQuantity(new BigDecimal("2"));
        duplicatedOrderRow.setRowKey(3);
        duplicatedOrderRow.setQuantity(new BigDecimal("3"));
        salesOrder.getLatestJson().getOrderRows().add(duplicatedOrderRow);
        salesOrderService.save(salesOrder, ORDER_CREATED);

        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        String invoiceEvent = readResource("examples/coreSalesInvoiceCreatedMultipleItems.json");

        //Replace order number with randomly created order number as expected
        invoiceEvent = invoiceEvent.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceEvent, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "445.60",
                "371.63",
                "445.60",
                "13.08",
                "10.99");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);

        SalesOrder originalSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        List<OrderRows> orderRows = originalSalesOrder.getLatestJson().getOrderRows();
        assertEquals(3, orderRows.size());
        assertEquals(2, orderRows.stream().filter(OrderRows::getIsCancelled).count());
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWithConsolidatedItems() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();
        String originalOrderNumber = salesOrder.getOrderNumber();
        OrderRows duplicatedOrderRow = objectUtil.deepCopyOf(salesOrder.getLatestJson().getOrderRows().get(0), OrderRows.class);
        duplicatedOrderRow.setRowKey(3);
        salesOrder.getLatestJson().getOrderRows().add(duplicatedOrderRow);
        salesOrderService.save(salesOrder, ORDER_CREATED);

        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        String invoiceEvent = readResource("examples/coreSalesInvoiceCreatedMultipleItems.json");

        //Replace order number with randomly created order number as expected
        invoiceEvent = invoiceEvent.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(invoiceEvent, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "445.60",
                "371.63",
                "445.60",
                "13.08",
                "10.99");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);

        SalesOrder originalSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        originalSalesOrder.getLatestJson().getOrderRows().stream().map(OrderRows::getIsCancelled).forEach(Assertions::assertTrue);

        SalesOrder subsequentSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber() + ORDER_NUMBER_SEPARATOR + invoiceNumber).orElseThrow();
        assertNotNull(subsequentSalesOrder.getInvoiceEvent());
        assertNotNull(subsequentSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertNotNull(subsequentSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber());
        assertEquals(subsequentSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId(),
                subsequentSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderGroupId());
        assertEquals(subsequentSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber(),
                subsequentSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
    }

    private boolean checkIfInvoiceCreatedReceivedProcessExists(String newOrderNumberCreatedInSoh) {
        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(newOrderNumberCreatedInSoh).orElse(null);
        if (updatedOrder == null || updatedOrder.getId() == null)
            return false;
        return !runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(updatedOrder.getId().toString())
                .variableValueEquals(ORDER_NUMBER.getName(), updatedOrder.getOrderNumber())
                .variableValueEquals(ORDER_GROUP_ID.getName(), updatedOrder.getOrderGroupId())
                .list().isEmpty();
    }

    private void checkTotalsValues(String orderNumber,
                                   String goodsTotalGross,
                                   String goodsTotalNet,
                                   String grandTotalGross,
                                   String grandTotalNet,
                                   String paymentTotal,
                                   String shippingGross,
                                   String shippingNet) {

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        Totals totals = updatedOrder.getLatestJson().getOrderHeader().getTotals();
        assertEquals(new BigDecimal(goodsTotalGross), totals.getGoodsTotalGross());
        assertEquals(new BigDecimal(goodsTotalNet), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal("0"), totals.getTotalDiscountGross());
        assertEquals(new BigDecimal("0"), totals.getTotalDiscountNet());
        assertEquals(new BigDecimal(grandTotalGross), totals.getGrandTotalGross());
        assertEquals(new BigDecimal(grandTotalNet), totals.getGrandTotalNet());
        assertEquals(new BigDecimal(paymentTotal), totals.getPaymentTotal());
        assertEquals(new BigDecimal(shippingGross), totals.getShippingCostGross());
        assertEquals(new BigDecimal(shippingNet), totals.getShippingCostNet());
        assertNotNull(totals.getSurcharges());
        assertNull(totals.getSurcharges().getDepositGross());
        assertNull(totals.getSurcharges().getDepositNet());
        assertNull(totals.getSurcharges().getBulkyGoodsGross());
        assertNull(totals.getSurcharges().getBulkyGoodsNet());
        assertNull(totals.getSurcharges().getRiskyGoodsGross());
        assertNull(totals.getSurcharges().getRiskyGoodsNet());
        assertNull(totals.getSurcharges().getPaymentGross());
        assertNull(totals.getSurcharges().getPaymentNet());
    }

    private void checkOrderRows(String orderNumber, String sku1, String sku2, String sku3) {
        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        List<OrderRows> orderRows = updatedOrder.getLatestJson().getOrderRows();
        checkOrderRowValues(
                orderRows.get(0),
                sku1,
                "2",
                "20",
                UnitValues.builder()
                        .goodsValueGross(new BigDecimal("201.0"))
                        .goodsValueNet(new BigDecimal("167.5"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .discountedGross(new BigDecimal("201.0"))
                        .discountedNet(new BigDecimal("167.5"))
                        .build(),
                SumValues.builder()
                        .goodsValueGross(new BigDecimal("402.0"))
                        .goodsValueNet(new BigDecimal("335.0"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .totalDiscountedGross(new BigDecimal("402.0"))
                        .totalDiscountedNet(new BigDecimal("335.0"))
                        .build(),
                "Unterdruckpumpe, Bremsanlage");
        checkOrderRowValues(
                orderRows.get(1),
                sku2,
                "2",
                "19",
                UnitValues.builder()
                        .goodsValueGross(new BigDecimal("10.00"))
                        .goodsValueNet(new BigDecimal("8.4"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .discountedGross(new BigDecimal("10.00"))
                        .discountedNet(new BigDecimal("8.4"))
                        .build(),
                SumValues.builder()
                        .goodsValueGross(new BigDecimal("20.00"))
                        .goodsValueNet(new BigDecimal("16.8"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .totalDiscountedGross(new BigDecimal("20.00"))
                        .totalDiscountedNet(new BigDecimal("16.8"))
                        .build(),
                "Luftfilter");
        checkOrderRowValues(
                orderRows.get(2),
                sku3,
                "1",
                "19",
                UnitValues.builder()
                        .goodsValueGross(new BigDecimal("10.52"))
                        .goodsValueNet(new BigDecimal("8.84"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .discountedGross(new BigDecimal("10.52"))
                        .discountedNet(new BigDecimal("8.84"))
                        .build(),
                SumValues.builder()
                        .goodsValueGross(new BigDecimal("10.52"))
                        .goodsValueNet(new BigDecimal("8.84"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .totalDiscountedGross(new BigDecimal("10.52"))
                        .totalDiscountedNet(new BigDecimal("8.84"))
                        .build(),
                sku3);
    }

    private void checkOrderRowValues(OrderRows row, String sku, String quantity, String taxRate,
                                     UnitValues expectedUnitValues, SumValues expectedSumValues, String description) {
        assertEquals(sku, row.getSku());
        assertEquals(new BigDecimal(quantity), row.getQuantity());
        assertEquals(new BigDecimal(taxRate), row.getTaxRate());
        assertEquals("shipment_regular", row.getShippingType());
        assertEquals(expectedUnitValues.getGoodsValueGross(), row.getUnitValues().getGoodsValueGross());
        assertEquals(expectedUnitValues.getGoodsValueNet(), row.getUnitValues().getGoodsValueNet());
        assertEquals(expectedUnitValues.getDiscountedGross(), row.getUnitValues().getDiscountedGross());
        assertEquals(expectedUnitValues.getDiscountedNet(), row.getUnitValues().getDiscountedNet());
        assertEquals(expectedSumValues.getGoodsValueGross(), row.getSumValues().getGoodsValueGross());
        assertEquals(expectedSumValues.getGoodsValueNet(), row.getSumValues().getGoodsValueNet());
        assertEquals(expectedSumValues.getTotalDiscountedGross(), row.getSumValues().getTotalDiscountedGross());
        assertEquals(expectedSumValues.getTotalDiscountedNet(), row.getSumValues().getTotalDiscountedNet());
        assertEquals(description, row.getName());
    }

    private void verifyThatNewRelicIsCalled(String newOrderNumberCreatedInSoh) {
        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(newOrderNumberCreatedInSoh).orElse(null);
        Map<String, Object> eventAttributes = new HashMap<>();
        assertThat(updatedOrder).isNotNull();
        var latestJson = updatedOrder.getLatestJson();
        eventAttributes.put("Platform", latestJson.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", latestJson.getOrderHeader().getSalesChannel());
        eventAttributes.put("SalesOrderNumber", latestJson.getOrderHeader().getOrderNumber());

        verify(insights).recordCustomEvent(eq("SohSubsequentOrderGenerated"),
                argThat(map -> {
                    assertThat((Map<String, Object>) map).containsAllEntriesOf(eventAttributes);
                    return true;
                }));
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }

    @AfterEach
    public void cleanup() {
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

}
