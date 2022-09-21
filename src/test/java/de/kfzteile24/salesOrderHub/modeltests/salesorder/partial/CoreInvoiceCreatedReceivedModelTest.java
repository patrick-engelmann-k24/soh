package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CoreInvoiceCreatedReceived model test")
@Slf4j(topic = "CoreInvoiceCreatedReceived model test")
class CoreInvoiceCreatedReceivedModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("CoreInvoiceCreatedReceivedTest"))
    @DisplayName("Start process before eventMsgOrderCoreSalesInvoiceCreated. Wait for the CoreSalesInvoiceCreated")
    void testEventMsgOrderCoreSalesInvoiceCreatedWaitingForMessage(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName(),
                businessKey, processVariables);

        verify(processScenario).waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName());
    }

    @Test
    @Tags(@Tag("CoreInvoiceCreatedReceivedTest"))
    @DisplayName("Start process before eventMsgOrderCoreSalesInvoiceCreated. CoreSalesInvoiceCreated received")
    void testEventMsgOrderCoreSalesInvoiceCreatedMessageReceived(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName()))
                .thenReturn(RECEIVED_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }
}
