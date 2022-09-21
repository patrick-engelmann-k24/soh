package de.kfzteile24.salesOrderHub.modeltests.returnorder;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.RETURN_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_CORE_CREDIT_NOTE_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_DROPSHIPMENT_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_PUBLISH_RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.RETURN_ORDER_MAIN_GATEWAY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PUBLISH_DELAY;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("ReturnOrderMainGateway model test")
@Slf4j(topic = "ReturnOrderMainGateway model test")
class ReturnOrderMainGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("ReturnOrderMainGateway"))
    @DisplayName("Start process with msgCoreCreditNoteCreated Message")
    void testReturnOrderMainGatewayCoreCreditNoteCreatedMessage(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(PUBLISH_DELAY.getName(), "PT10S");

        scenario = startBeforeActivity(RETURN_ORDER_PROCESS, START_MSG_CORE_CREDIT_NOTE_CREATED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(RETURN_ORDER_MAIN_GATEWAY.getName());
        verify(processScenario).hasCompleted(START_MSG_CORE_CREDIT_NOTE_CREATED.getName());
        verify(processScenario, never()).hasCompleted(START_MSG_DROPSHIPMENT_ORDER_RETURN_CONFIRMED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_PUBLISH_RETURN_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(END_MSG_CORE_CREDIT_NOTE_RECEIVED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }

    @Test
    @Tags(@Tag("ReturnOrderMainGateway"))
    @DisplayName("Start process with msgDropshipmentOrderReturnConfirmed Message")
    void testReturnOrderMainGatewayDropshipmentOrderReturnConfirmedMessage(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(PUBLISH_DELAY.getName(), "PT10S");

        scenario = startBeforeActivity(RETURN_ORDER_PROCESS, START_MSG_DROPSHIPMENT_ORDER_RETURN_CONFIRMED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(RETURN_ORDER_MAIN_GATEWAY.getName());
        verify(processScenario).hasCompleted(START_MSG_DROPSHIPMENT_ORDER_RETURN_CONFIRMED.getName());
        verify(processScenario, never()).hasCompleted(START_MSG_CORE_CREDIT_NOTE_CREATED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_PUBLISH_RETURN_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(END_MSG_CORE_CREDIT_NOTE_RECEIVED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }
}
