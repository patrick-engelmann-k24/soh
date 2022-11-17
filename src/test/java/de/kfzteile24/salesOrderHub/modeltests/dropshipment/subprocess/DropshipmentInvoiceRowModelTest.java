package de.kfzteile24.salesOrderHub.modeltests.dropshipment.subprocess;

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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_INVOICE_ROW_CREATE_INVOICE_ENTRY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_INVOICE_ROW_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_INVOICE_ROW_PUBLISH_TRACKING_INFORMATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_INVOICE_ROW_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DropshipmentInvoiceRowSubProcess model test")
@Slf4j(topic = "DropshipmentInvoiceRowSubProcess model test")
class DropshipmentInvoiceRowModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }


    @Test
    @Tags(@Tag("DropshipmentInvoiceRowSubProcessTest"))
    @DisplayName("Start process before eventMsgDropShipmentOrderConfirmed. isDropshipmentOrderConfirmed is true")
    void testDropshipmentInvoiceRowSubprocess(TestInfo testinfo){
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true);

        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_INVOICE_ROW_TRACKING_INFORMATION_RECEIVED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.runsCallActivity(DROPSHIPMENT_INVOICE_ROW_PROCESS.getName()))
                .thenReturn(executeCallActivity());

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName(),
                businessKey, processVariables);

        verify(processScenario, times(3)).hasCompleted(DROPSHIPMENT_INVOICE_ROW_CREATE_INVOICE_ENTRY.getName());
        verify(processScenario, times(3)).hasCompleted(DROPSHIPMENT_INVOICE_ROW_PUBLISH_TRACKING_INFORMATION.getName());
        verify(processScenario, times(3)).hasCompleted(DROPSHIPMENT_INVOICE_ROW_PUBLISH_TRACKING_INFORMATION.getName());

        assertThat(scenario.instance(processScenario)).isEnded();

    }

}