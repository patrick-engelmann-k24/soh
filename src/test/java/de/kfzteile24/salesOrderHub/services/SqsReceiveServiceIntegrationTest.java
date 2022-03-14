package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.Totals;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author stefand
 */

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class
)
public class SqsReceiveServiceIntegrationTest {

    @Autowired
    private CamundaHelper camundaHelper;
    @Autowired
    private SqsReceiveService sqsReceiveService;
    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private SalesOrderService salesOrderService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private ProcessEngine processEngine;

    @BeforeEach
    public void setup() {
        reset();
        init(processEngine);
        bpmUtil.cleanUp();
    }

    @Test
    public void testQueueListenerCoreCancellationNotFullyCancelled() {

        var senderId = "Ecp";
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        bpmUtil.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, salesOrder.getOrderNumber());

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13012")));

        String cancellationRawMessage = readResource("examples/coreCancellationOneRowMessage.json");
        sqsReceiveService.queueListenerCoreCancellation(cancellationRawMessage, senderId, 1);

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));
        assertFalse(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13012")));

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);
        assertNotNull(updatedOrder);
        Totals totals = updatedOrder.getLatestJson().getOrderHeader().getTotals();
        assertEquals(new BigDecimal("100.00"), totals.getGoodsTotalGross());
        assertEquals(new BigDecimal("100.00"), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal("37.49"), totals.getTotalDiscountGross());
        assertEquals(new BigDecimal("19.50"), totals.getTotalDiscountNet());
        assertEquals(new BigDecimal("62.51"), totals.getGrandTotalGross());
        assertEquals(new BigDecimal("80.50"), totals.getGrandTotalNet());
        assertEquals(new BigDecimal("62.51"), totals.getPaymentTotal());

        //cleanup to remove the uncancelled process
        runtimeService.deleteProcessInstance(orderProcess.getProcessInstanceId(), "test");
    }

    @Test
    public void testQueueListenerCoreCancellationFullyCancelled() {

        var senderId = "Ecp";
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        ProcessInstance orderProcessInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);
        bpmUtil.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));
        assertTrue(timerService.pollWithDefaultTiming(() -> camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), "2270-13012")));

        String cancellationRawMessage = readResource("examples/coreCancellationTwoRowsMessage.json");
        sqsReceiveService.queueListenerCoreCancellation(cancellationRawMessage, senderId, 1);

        final var hasProcessEnded = timerService.pollWithDefaultTiming(() -> {
            BpmnAwareTests.assertThat(orderProcessInstance).isEnded();
            return true;
        });
        assertThat(hasProcessEnded).isTrue();

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElse(null);
        assertNotNull(updatedOrder);
        Totals totals = updatedOrder.getLatestJson().getOrderHeader().getTotals();
        assertEquals(new BigDecimal("86.69"), totals.getGoodsTotalGross());
        assertEquals(new BigDecimal("88.82"), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal("24.58"), totals.getTotalDiscountGross());
        assertEquals(new BigDecimal("8.66"), totals.getTotalDiscountNet());
        assertEquals(new BigDecimal("62.11"), totals.getGrandTotalGross());
        assertEquals(new BigDecimal("80.16"), totals.getGrandTotalNet());
        assertEquals(new BigDecimal("62.11"), totals.getPaymentTotal());
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
        bpmUtil.cleanUp();
    }

}
