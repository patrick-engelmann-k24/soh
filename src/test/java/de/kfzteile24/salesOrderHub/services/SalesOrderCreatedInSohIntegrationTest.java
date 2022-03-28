package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Totals;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.ProcessEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private TimedPollingService timerService;

    @BeforeEach
    public void setup() {
        init(processEngine);
    }

    @Test
    public void testQueueListenerSubsequentDeliveryNote() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String subDeliveryOrderNumber = "111001110";
        String rowSku = "2010-10183";
        String subsequentDeliveryMsg = readResource("examples/subsequentDeliveryNoteWithOneItem.json");

        //Replace order number with randomly created order number as expected
        subsequentDeliveryMsg = subsequentDeliveryMsg.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerSubsequentDeliveryReceived(subsequentDeliveryMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = originalOrderNumber + "-" + subDeliveryOrderNumber;
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "12.95",
                "10.79",
                "1.94",
                "1.62",
                "11.01",
                "9.17",
                "11.01");
    }

    @Test
    public void testQueueListenerSubsequentDeliveryNoteWithMultipleItems() {

        var senderId = "Delivery";
        var receiveCount = 1;
        var salesOrder = salesOrderUtil.createNewSalesOrderHavingCancelledRow();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String subDeliveryOrderNumber = "111001110";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        String subsequentDeliveryMsg = readResource("examples/subsequentDeliveryNoteWithMultipleItems.json");

        //Replace order number with randomly created order number as expected
        subsequentDeliveryMsg = subsequentDeliveryMsg.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerSubsequentDeliveryReceived(subsequentDeliveryMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = originalOrderNumber + "-" + subDeliveryOrderNumber;
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku1)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku2)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku3)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "60.30",
                "50.26",
                "372.22",
                "310.38",
                "372.22");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);
    }

    private void checkTotalsValues(String orderNumber,
                                   String goodsTotalGross,
                                   String goodsTotalNet,
                                   String totalDiscountGross,
                                   String totalDiscountNet,
                                   String grandTotalGross,
                                   String grandTotalNet,
                                   String paymentTotal) {

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        Totals totals = updatedOrder.getLatestJson().getOrderHeader().getTotals();
        assertEquals(new BigDecimal(goodsTotalGross), totals.getGoodsTotalGross());
        assertEquals(new BigDecimal(goodsTotalNet), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal(totalDiscountGross), totals.getTotalDiscountGross());
        assertEquals(new BigDecimal(totalDiscountNet), totals.getTotalDiscountNet());
        assertEquals(new BigDecimal(grandTotalGross), totals.getGrandTotalGross());
        assertEquals(new BigDecimal(grandTotalNet), totals.getGrandTotalNet());
        assertEquals(new BigDecimal(paymentTotal), totals.getPaymentTotal());
        assertNull(totals.getShippingCostGross());
        assertNull(totals.getShippingCostNet());
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
                1,
                "2",
                "20.0",
                new BigDecimal("201.00"),
                new BigDecimal("167.50"),
                new BigDecimal("30.15"),
                new BigDecimal("25.13"),
                new BigDecimal("170.85"),
                new BigDecimal("142.37"),
                new BigDecimal("402.00"),
                new BigDecimal("335.00"),
                new BigDecimal("60.30"),
                new BigDecimal("50.26"),
                new BigDecimal("341.70"),
                new BigDecimal("284.74"));
        checkOrderRowValues(
                orderRows.get(1),
                sku2,
                2,
                "2",
                "19",
                new BigDecimal("10"),
                new BigDecimal("8.40"),
                new BigDecimal("0"),
                new BigDecimal("0"),
                new BigDecimal("10"),
                new BigDecimal("8.40"),
                new BigDecimal("20"),
                new BigDecimal("16.80"),
                new BigDecimal("0"),
                new BigDecimal("0"),
                new BigDecimal("20"),
                new BigDecimal("16.80"));
        checkOrderRowValues(
                orderRows.get(2),
                sku3,
                3,
                "1",
                "19",
                new BigDecimal("10.52"),
                new BigDecimal("8.84"),
                null,
                null,
                new BigDecimal("10.52"),
                new BigDecimal("8.84"),
                new BigDecimal("10.52"),
                new BigDecimal("8.84"),
                null,
                null,
                new BigDecimal("10.52"),
                new BigDecimal("8.84"));
    }

    private void checkOrderRowValues(OrderRows row,
                                     String sku,
                                     Integer rowKey,
                                     String quantity,
                                     String taxRate,
                                     BigDecimal unitGoodsValueGross,
                                     BigDecimal unitGoodsValueNet,
                                     BigDecimal unitDiscountGross,
                                     BigDecimal unitDiscountNet,
                                     BigDecimal unitDiscountedGross,
                                     BigDecimal unitDiscountedNet,
                                     BigDecimal sumGoodsValueGross,
                                     BigDecimal sumGoodsValueNet,
                                     BigDecimal sumDiscountGross,
                                     BigDecimal sumDiscountNet,
                                     BigDecimal sumTotalDiscountedGross,
                                     BigDecimal sumTotalDiscountedNet) {
        assertEquals(sku, row.getSku());
        assertEquals(rowKey, row.getRowKey());
        assertEquals("shipment_regular", row.getShippingType());
        assertEquals(new BigDecimal(quantity), row.getQuantity());
        assertEquals(new BigDecimal(taxRate), row.getTaxRate());
        assertEquals(unitGoodsValueGross, row.getUnitValues().getGoodsValueGross());
        assertEquals(unitGoodsValueNet, row.getUnitValues().getGoodsValueNet());
        assertEquals(unitDiscountGross, row.getUnitValues().getDiscountGross());
        assertEquals(unitDiscountNet, row.getUnitValues().getDiscountNet());
        assertEquals(unitDiscountedGross, row.getUnitValues().getDiscountedGross());
        assertEquals(unitDiscountedNet, row.getUnitValues().getDiscountedNet());
        assertEquals(sumGoodsValueGross, row.getSumValues().getGoodsValueGross());
        assertEquals(sumGoodsValueNet, row.getSumValues().getGoodsValueNet());
        assertEquals(sumDiscountGross, row.getSumValues().getDiscountGross());
        assertEquals(sumDiscountNet, row.getSumValues().getDiscountNet());
        assertEquals(sumTotalDiscountedGross, row.getSumValues().getTotalDiscountedGross());
        assertEquals(sumTotalDiscountedNet, row.getSumValues().getTotalDiscountedNet());
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
