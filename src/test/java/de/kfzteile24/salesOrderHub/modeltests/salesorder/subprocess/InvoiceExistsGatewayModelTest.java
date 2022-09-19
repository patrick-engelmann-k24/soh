package de.kfzteile24.salesOrderHub.modeltests.salesorder.subprocess;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.CHANGE_INVOICE_ADDRESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_INVOICE_ADDRESS_CHANGED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.INVOICE_ADDRESS_NOT_CHANGED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_INVOICE_EXIST;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_EXISTS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("InvoiceExistsGateway model test")
@Slf4j(topic = "InvoiceExistsGateway model test")
class InvoiceExistsGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("InvoiceExistsGatewayTest"))
    @DisplayName("Start process before gwXORInvoiceExist. invoiceExist is true")
    void testInvoiceExistsGatewayTestInvoiceExistsTrue(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(INVOICE_EXISTS.getName(), true);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_INVOICE_EXIST.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted( XOR_INVOICE_EXIST.getName());
        verify(processScenario, never()).hasCompleted(CHANGE_INVOICE_ADDRESS.getName());
        verify(processScenario, never()).hasCompleted(END_MSG_INVOICE_ADDRESS_CHANGED.getName());
        verify(processScenario).hasCompleted(INVOICE_ADDRESS_NOT_CHANGED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }

    @Test
    @Tags(@Tag("InvoiceExistsGatewayTest"))
    @DisplayName("Start process before gwXORInvoiceExist. invoiceExist is false")
    void testInvoiceExistsGatewayTestInvoiceExistsFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(INVOICE_EXISTS.getName(), Boolean.FALSE);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_INVOICE_EXIST.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted( XOR_INVOICE_EXIST.getName());
        verify(processScenario, never()).hasCompleted(INVOICE_ADDRESS_NOT_CHANGED.getName());
        verify(processScenario).hasCompleted(CHANGE_INVOICE_ADDRESS.getName());
        verify(processScenario).hasCompleted(END_MSG_INVOICE_ADDRESS_CHANGED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }
}