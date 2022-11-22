package de.kfzteile24.salesOrderHub.modeltests.dropshipment.invoice;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.INVOICING_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_INVOICING_DROPSHIPMENT_ORDER_FULLY_INVOICED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_INVOICING_GENERATE_FULLY_INVOICED_PDF;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_INVOICING_PUBLISH_FULLY_INVOICED_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("DropshipmentOrderFullyInvoiced Invoicing SubProcess model test")
@Slf4j(topic = "DropshipmentOrderFullyInvoiced Invoicing SubProcess model test")
class DropshipmentOrderFullyInvoicedModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }


    @Test
    @Tags(@Tag("DropshipmentOrderFullyInvoicedTest"))
    @DisplayName("Start process before activityAggregateInvoiceData. isPartialInvoice is false")
    void testDropshipmentOrderFullyInvoiced(TestInfo testinfo){
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_PARTIAL_INVOICE.getName(), false);

        scenario = startBeforeActivity(INVOICING_PROCESS, XOR_CHECK_PARTIAL_INVOICE.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE.getName());
        verify(processScenario).hasCompleted(EVENT_THROW_MSG_INVOICING_PUBLISH_FULLY_INVOICED_DATA.getName());
        verify(processScenario).hasCompleted(EVENT_THROW_MSG_INVOICING_DROPSHIPMENT_ORDER_FULLY_INVOICED.getName());
        verify(processScenario).hasCompleted(EVENT_THROW_MSG_INVOICING_GENERATE_FULLY_INVOICED_PDF.getName());

        assertThat(scenario.instance(processScenario)).isEnded();

    }

}