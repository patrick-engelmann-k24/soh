package de.kfzteile24.salesOrderHub.modeltests.invoicing;

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
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.CALL_ACTIVITY_DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.CREATE_DROPSHIPMENT_SUBSEQUENT_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_GENERATE_PARTLY_INVOICED_PDF;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PUBLISH_PARTLY_INVOICED_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DropshipmentOrderPartiallyInvoiced Invoicing SubProcess model test")
@Slf4j(topic = "DropshipmentOrderPartiallyInvoiced Invoicing SubProcess model test")
public class DropshipmentOrderPartiallyInvoicedModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }


    @Test
    @Tags(@Tag("DropshipmentOrderPartiallyInvoicedTest"))
    @DisplayName("Start process before activityAggregateInvoiceData. isPartialInvoice is true")
    void testDropshipmentOrderPartiallyInvoiced(TestInfo testinfo){
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_PARTIAL_INVOICE.getName(), true);
        processVariables.put(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true);

        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.runsCallActivity(DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS.getName()))
                .thenReturn(executeCallActivity());
        when(processScenario.waitsAtEventBasedGateway(EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE.getName()))
                .thenReturn(RECEIVED_SIGNAL_EVENT_GATEWAY_ACTION);
        when(processScenario.runsCallActivity(CALL_ACTIVITY_DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName()))
                .thenReturn(executeCallActivity());

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName(),
                businessKey, processVariables);

        scenario = startBeforeActivity(INVOICING_PROCESS, XOR_CHECK_PARTIAL_INVOICE.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(CREATE_DROPSHIPMENT_SUBSEQUENT_INVOICE.getName());
        verify(processScenario).hasCompleted(CALL_ACTIVITY_DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName());
        verify(processScenario).hasCompleted(EVENT_THROW_MSG_PUBLISH_PARTLY_INVOICED_DATA.getName());
        verify(processScenario).hasCompleted(EVENT_THROW_MSG_GENERATE_PARTLY_INVOICED_PDF.getName());

        assertThat(scenario.instance(processScenario)).isEnded();

    }
}
