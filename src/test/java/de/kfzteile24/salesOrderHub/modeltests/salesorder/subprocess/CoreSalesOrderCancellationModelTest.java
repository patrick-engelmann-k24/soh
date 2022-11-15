package de.kfzteile24.salesOrderHub.modeltests.salesorder.subprocess;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.SUB_PROCESS_CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;


@DisplayName("CoreSalesOrderCancellationSubprocess model test")
@Slf4j(topic = "CoreSalesOrderCancellationSubprocess model test")
class CoreSalesOrderCancellationModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }


    @Test
    @Tags(@Tag("CoreSalesOrderCancellationSubprocessTest"))
    @DisplayName("Start process before gwXORInvoiceExist. invoiceExist is true") //TODO
    void testCoreSalesOrderCancellationSubprocess(TestInfo testinfo){
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, START_MSG_CORE_SALES_ORDER_CANCELLED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(START_MSG_CORE_SALES_ORDER_CANCELLED.getName());
        verify(processScenario).hasCompleted(Activities.CORE_SALES_ORDER_CANCELLED.getName());
        verify(processScenario).hasCompleted(END_MSG_CORE_SALES_ORDER_CANCELLED.getName());
        verify(processScenario).hasCompleted(SUB_PROCESS_CORE_SALES_ORDER_CANCELLED.getName());
        verify(processScenario, never()).hasCompleted(MSG_ORDER_PAYMENT_SECURED.getName());
        verify(processScenario, never()).hasCompleted(END_MSG_ORDER_COMPLETED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();

    }
}
