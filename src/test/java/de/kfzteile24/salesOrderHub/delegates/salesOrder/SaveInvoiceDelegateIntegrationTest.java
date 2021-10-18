package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_MARKETPLACE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_VALID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderInvoice;
import static java.util.Collections.singletonList;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class
)
public class SaveInvoiceDelegateIntegrationTest {
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderInvoiceRepository salesOrderInvoiceRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private TransactionTemplate txTemplate;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testInvoiceIsStoredCorrectlyWithSubsequentInvoice() {
        final var testOrder = salesOrderUtil.createNewSalesOrder();
        final var orderProcess = createOrderProcess(testOrder);
        final var orderNumber = testOrder.getOrderNumber();

        final var expectedInvoice = createSalesOrderInvoice(orderNumber, false);

        var firstInvoiceProcess = createSaveInvoiceProcess(orderNumber, expectedInvoice);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertInvoices(orderNumber, singletonList(expectedInvoice));

        assertThat(firstInvoiceProcess).isEnded()
                                       .hasPassedInOrder(Events.START_MSG_INVOICE_CREATED.getName(),
                                                         Activities.SAVE_INVOICE.getName(),
                                                         Events.INVOICE_SAVED.getName());

        // add a correction invoice
        final var expectedCorrectionInvoice = createSalesOrderInvoice(orderNumber, true);

        var correctedInvoiceProcess = createSaveInvoiceProcess(orderNumber, expectedCorrectionInvoice);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertInvoices(orderNumber, Arrays.asList(expectedInvoice, expectedCorrectionInvoice));

        assertThat(correctedInvoiceProcess).isEnded()
                                           .hasPassedInOrder(Events.START_MSG_INVOICE_CREATED.getName(),
                                                             Activities.SAVE_INVOICE.getName(),
                                                             Events.INVOICE_SAVED.getName());


        util.finishOrderProcess(orderProcess, orderNumber);

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), INVOICE_RECEIVED, 2);
    }

    @Test
    public void testInvoiceIsStoredCorrectlyWithSubsequentInvoiceAfterOrderProcessIsCompleted() {
        final var testOrder = salesOrderUtil.createNewSalesOrder();
        final var orderProcess = createOrderProcess(testOrder);
        final var orderNumber = testOrder.getOrderNumber();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        util.finishOrderProcess(orderProcess, orderNumber);

        final var expectedInvoice = createSalesOrderInvoice(orderNumber, false);

        var firstInvoiceProcess = createSaveInvoiceProcess(orderNumber, expectedInvoice);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertInvoices(orderNumber, singletonList(expectedInvoice));

        assertThat(firstInvoiceProcess).isEnded()
                                       .hasPassedInOrder(Events.START_MSG_INVOICE_CREATED.getName(),
                                                         Activities.SAVE_INVOICE.getName(),
                                                         Events.INVOICE_SAVED.getName());

        // add a correction invoice
        final var expectedCorrectionInvoice = createSalesOrderInvoice(orderNumber, true);

        var correctedInvoiceProcess = createSaveInvoiceProcess(orderNumber, expectedCorrectionInvoice);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertInvoices(orderNumber, Arrays.asList(expectedInvoice, expectedCorrectionInvoice));

        assertThat(correctedInvoiceProcess).isEnded()
                                           .hasPassedInOrder(Events.START_MSG_INVOICE_CREATED.getName(),
                                                             Activities.SAVE_INVOICE.getName(),
                                                             Events.INVOICE_SAVED.getName());

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), INVOICE_RECEIVED, 2);
    }

    @Test
    public void invoicesAreAlsoStoredWhenNoCorrespondingSalesOrderExistsYet() {
        final var testOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var orderNumber = testOrder.getOrderNumber();
        final var invoice = createSalesOrderInvoice(orderNumber, false);
        createSaveInvoiceProcess(orderNumber, invoice);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final var storedInvoices = salesOrderInvoiceRepository.getInvoicesByOrderNumber(orderNumber);
        assertEquals(1, storedInvoices.size());
    }

    private void assertInvoices(final String orderNumber, final List<SalesOrderInvoice> expectedInvoices) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                final var salesOrderOpt = salesOrderRepository.getOrderByOrderNumber(orderNumber);
                assertTrue(salesOrderOpt.isPresent(), "Sales order not found");
                final var salesOrder = salesOrderOpt.get();
                assertEquals(expectedInvoices.size(),
                             salesOrder.getSalesOrderInvoiceList().size());

                expectedInvoices.forEach(expected -> {
                    final var actualOpt = salesOrder.getSalesOrderInvoiceList()
                                                    .stream().filter(i -> i.getInvoiceNumber().equals(expected.getInvoiceNumber()))
                                                    .findAny();
                    assertTrue(actualOpt.isPresent());

                    final var actual = actualOpt.get();
                    assertEquals(expected.getInvoiceNumber(), actual.getInvoiceNumber());
                    assertEquals(expected.getUrl(), actual.getUrl());
                    assertNotNull(actual.getCreatedAt());
                    assertNotNull(actual.getUpdatedAt());
                    assertEquals(salesOrder.getOrderNumber(), actual.getOrderNumber());
                });
            }
        });
    }

    private ProcessInstance createOrderProcess(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(SHIPMENT_METHOD.getName(), REGULAR.getName());
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(PAYMENT_TYPE.getName(), CREDIT_CARD.getName());
        processVariables.put(ORDER_VALID.getName(), true);
        processVariables.put(ORDER_ROWS.getName(), orderItems);

        return runtimeService.createMessageCorrelation(ORDER_RECEIVED_MARKETPLACE.getName())
                             .processInstanceBusinessKey(orderNumber)
                             .setVariables(processVariables)
                             .correlateWithResult()
                             .getProcessInstance();
    }

    private ProcessInstance createSaveInvoiceProcess(String orderNumber, SalesOrderInvoice expectedInvoice) {
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(INVOICE_URL.getName(), expectedInvoice.getUrl());
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);

        return runtimeService.createMessageCorrelation(INVOICE_CREATED.getName())
                             .processInstanceVariableEquals(ORDER_NUMBER.getName(), orderNumber)
                             .setVariables(processVariables)
                             .correlateWithResult()
                             .getProcessInstance();
    }

    @AfterEach
    public void cleanup() {
        salesOrderInvoiceRepository.deleteAll();
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
    }
}
