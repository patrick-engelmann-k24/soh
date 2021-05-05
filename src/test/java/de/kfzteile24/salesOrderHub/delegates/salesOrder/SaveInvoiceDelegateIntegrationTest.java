package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_MARKETPLACE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_VALID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.PACKING_STARTED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.TRACKING_ID_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static java.util.Collections.singletonList;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class SaveInvoiceDelegateIntegrationTest {
    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderInvoiceRepository salesOrderInvoiceRepository;

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
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final var expectedInvoice = createSalesOrderInvoice(orderNumber, false);
        util.sendMessage(INVOICE_CREATED.getName(), orderNumber,
                Map.of(INVOICE_URL.getName(), expectedInvoice.getUrl()));

        assertInvoices(orderNumber, singletonList(expectedInvoice));

        // add a correction invoice
        final var expectedCorrectionInvoice = createSalesOrderInvoice(orderNumber, true);
        util.sendMessage(INVOICE_CREATED.getName(), orderNumber,
                Map.of(INVOICE_URL.getName(), expectedCorrectionInvoice.getUrl()));

        assertInvoices(orderNumber, Arrays.asList(expectedInvoice, expectedCorrectionInvoice));

        assertThat(orderProcess).hasPassedInOrder(
                util._N(Events.START_MSG_INVOICE_CREATED),
                util._N(Activities.SAVE_INVOICE),
                util._N(Events.INVOICE_SAVED)
        );

        finishOrderProcess(orderProcess, orderNumber);

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), INVOICE_RECEIVED, 2);
    }

    private SalesOrderInvoice createSalesOrderInvoice(final String orderNumber, final boolean isCorrection) {
        final var sep = isCorrection ? "--" : "-";
        final var invoiceNumber = RandomStringUtils.randomNumeric(10);
        final var invoiceUrl = "s3://k24-invoices/www-k24-at/2020/08/12/" + orderNumber + sep + invoiceNumber + ".pdf";
        return SalesOrderInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .url(invoiceUrl)
                .build();
    }

    private void assertInvoices(final String orderNumber, final List<SalesOrderInvoice> expectedInvoices) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {

            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                final var salesOrderOpt = salesOrderRepository.getOrderByOrderNumber(orderNumber);
                assertTrue( salesOrderOpt.isPresent(), "Sales order not found");
                final var salesOrder = salesOrderOpt.get();
                assertEquals(expectedInvoices.size(),
                    salesOrder.getSalesOrderInvoiceList().size());

                expectedInvoices.forEach( expected -> {
                    final var actualOpt = salesOrder.getSalesOrderInvoiceList()
                            .stream().filter(i -> i.getInvoiceNumber().equals(expected.getInvoiceNumber()))
                            .findAny();
                    assertTrue(actualOpt.isPresent());

                    final var actual = actualOpt.get();
                    assertEquals(expected.getInvoiceNumber(), actual.getInvoiceNumber());
                    assertEquals(expected.getUrl(), actual.getUrl());
                    assertNotNull(actual.getCreatedAt());
                    assertNotNull(actual.getUpdatedAt());
                });
            }
        });
    }

    private ProcessInstance createOrderProcess(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
        processVariables.put(util._N(ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(PAYMENT_TYPE), util._N(CREDIT_CARD));
        processVariables.put(util._N(ORDER_VALID), true);
        processVariables.put(util._N(ORDER_ROWS), orderItems);

        return runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                .processInstanceBusinessKey(orderNumber)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }

    void finishOrderProcess(final ProcessInstance orderProcess, final String orderNumber) {
        // start subprocess
        util.sendMessage(util._N(ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        // send items thru
        util.sendMessage(util._N(ROW_TRANSMITTED_TO_LOGISTICS), orderNumber);
        util.sendMessage(util._N(PACKING_STARTED), orderNumber);
        util.sendMessage(util._N(TRACKING_ID_RECEIVED), orderNumber);
        util.sendMessage(util._N(ROW_SHIPPED), orderNumber);

        assertThat(orderProcess).isEnded();
    }
}
