package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

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

    private SalesOrder testOrder;

    @BeforeEach
    public void setup() {
        init(processEngine);
        testOrder = salesOrderUtil.createNewSalesOrder();
    }

    @Test
    public void testQueueListenerSubsequentDeliveryNote() {

        var senderId = "Delivery";
        var receiveCount = 1;

        String originalOrderNumber = testOrder.getOrderNumber();
        String subDeliveryOrderNumber = "111001110";
        String rowSku = "2010-10183";
        String subsequentDeliveryMsg = readResource("examples/subsequentDeliveryNote.json");

        //Replace order number with randomly created order number as expected
        subsequentDeliveryMsg = subsequentDeliveryMsg.replace("524001248", originalOrderNumber);

        sqsReceiveService.queueListenerSubsequentDeliveryReceived(subsequentDeliveryMsg, senderId, receiveCount);

        String newOrderNumberCreatedInSoh = originalOrderNumber + "-" + subDeliveryOrderNumber;
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(newOrderNumberCreatedInSoh)));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(newOrderNumberCreatedInSoh, rowSku)));
        checkTotalsValues(newOrderNumberCreatedInSoh);
    }

    private void checkTotalsValues(String orderNumber) {

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        Totals totals = updatedOrder.getLatestJson().getOrderHeader().getTotals();
        assertEquals(BigDecimal.valueOf(12.95), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.valueOf(10.79), totals.getGoodsTotalNet());
        assertEquals(BigDecimal.valueOf(11.01), totals.getTotalDiscountGross());
        assertEquals(BigDecimal.valueOf(9.17), totals.getTotalDiscountNet());
        assertEquals(BigDecimal.valueOf(1.94), totals.getGrandTotalGross());
        assertEquals(BigDecimal.valueOf(1.62), totals.getGrandTotalNet());
        assertEquals(BigDecimal.valueOf(1.94), totals.getPaymentTotal());
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
