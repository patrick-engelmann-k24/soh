package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.SAVE_CREDIT_NOTE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.CREDIT_NOTE_SAVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class SaveCreditNoteDelegateIntegrationTest extends AbstractWorkflowTest {
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private SalesOrderReturnService salesOrderReturnService;

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private TimedPollingService pollingService;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    @DisplayName("When Save Credit Note Process Starts Then It Ends Successfully And No Exceptions Are Thrown")
    void whenSaveCreditNoteProcessStartsThenItEndsSuccessfullyAndNoExceptionsAreThrown() {
        var expectedInvoiceUrl = "test";
        String orderRawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        Order order = getOrder(orderRawMessage);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(order));
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");
        salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);

        var creditNoteProcess = createSaveCreditNoteProcess(salesOrderReturn.getOrderNumber(), expectedInvoiceUrl);

        final var creditNoteProcessEndedCorrectly = pollingService.pollWithDefaultTiming(() -> {
            assertThat(creditNoteProcess).isEnded()
                    .hasPassedInOrder(START_MSG_CREDIT_NOTE_CREATED.getName(),
                            SAVE_CREDIT_NOTE.getName(),
                            CREDIT_NOTE_SAVED.getName());
            return true;
        });

        assertTrue(creditNoteProcessEndedCorrectly);

    }

    private ProcessInstance createSaveCreditNoteProcess(String orderNumber, String expectedInvoiceUrl) {
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(INVOICE_URL.getName(), expectedInvoiceUrl);
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);

        return runtimeService.createMessageCorrelation(DROPSHIPMENT_CREDIT_NOTE_CREATED.getName())
                .setVariables(processVariables)
                .correlateWithResult()
                .getProcessInstance();
    }

    @AfterEach
    public void cleanup() {
        salesOrderReturnRepository.deleteAll();
        salesOrderRepository.deleteAll();
    }
}
