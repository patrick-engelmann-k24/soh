package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.SAVE_CREDIT_NOTE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.CREDIT_NOTE_URL_SAVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_CREDIT_NOTE_DOCUMENT_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_CREDIT_NOTE_DOCUMENT_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveCreditNoteUrlDelegateIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    @DisplayName("When Save Credit Note Url Process Starts Then It Ends Successfully And No Exceptions Are Thrown")
    void whenSaveCreditNoteUrlProcessStartsThenItEndsSuccessfullyAndNoExceptionsAreThrown() {
        var expectedInvoiceUrl = "test";
        var orderNumber = UUID.randomUUID().toString();
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        message.getOrderHeader().setOrderNumber(orderNumber);
        message.getOrderHeader().setOrderGroupId(orderNumber);
        SalesOrder salesOrder = salesOrderService.createSalesOrder(createSalesOrderFromOrder(message));
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "123");
        salesOrderReturnService.save(salesOrderReturn, RETURN_ORDER_CREATED);

        var creditNoteProcess = createSaveCreditNoteUrlProcess(salesOrderReturn.getOrderNumber(), expectedInvoiceUrl);

        final var creditNoteProcessEndedCorrectly = pollingService.pollWithDefaultTiming(() -> {
            assertThat(creditNoteProcess).isEnded()
                    .hasPassedInOrder(START_MSG_CREDIT_NOTE_DOCUMENT_GENERATED.getName(),
                            SAVE_CREDIT_NOTE_URL.getName(),
                            CREDIT_NOTE_URL_SAVED.getName());
            return true;
        });

        assertTrue(creditNoteProcessEndedCorrectly);

    }

    private ProcessInstance createSaveCreditNoteUrlProcess(String orderNumber, String expectedInvoiceUrl) {
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(INVOICE_URL.getName(), expectedInvoiceUrl);
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);

        return runtimeService.createMessageCorrelation(DROPSHIPMENT_CREDIT_NOTE_DOCUMENT_GENERATED.getName())
                .setVariables(processVariables)
                .correlateWithResult()
                .getProcessInstance();
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> salesOrderReturnRepository.deleteAll());
    }
}
