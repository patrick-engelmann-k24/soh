package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCompletedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckOrderTypeDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckPlatformTypeDelegate;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.mock.mockito.MockBean;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_BRANCH_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_BRANCH_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_SOH_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IsSohOrderGateway model test")
@Slf4j(topic = "IsSohOrderGateway model test")
@MockBean({
        CheckPlatformTypeDelegate.class,
        CheckOrderTypeDelegate.class,
        OrderCompletedDelegate.class
})
class IsSohOrderGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("IsSohOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckPlatformType. isSohOrder is true")
    void testIsSohOrderGatewayTestisSohOrderTrue(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_SOH_ORDER.getName(), true);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(ROW_TRANSMITTED_TO_LOGISTICS.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);
        when(processScenario.runsCallActivity(ORDER_ROW_FULFILLMENT_PROCESS.getName()))
                .thenReturn(executeCallActivity());

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_PLATFORM_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario, never()).hasCompleted(XOR_CHECK_BRANCH_TYPE.getName());
        verify(processScenario, times(3)).waitsAtMockedCallActivity(ORDER_ROW_FULFILLMENT_PROCESS.getName());
        verify(processScenario, times(3)).waitsAtMessageIntermediateCatchEvent(ROW_TRANSMITTED_TO_LOGISTICS.getName());
    }

    @Test
    @Tags(@Tag("IsSohOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckPlatformType. isSohOrder is false, isBranchOrder is true")
    void testIsSohOrderGatewayTestisSohOrderFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_SOH_ORDER.getName(), false);
        processVariables.put(IS_BRANCH_ORDER.getName(), true);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_PLATFORM_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(XOR_CHECK_BRANCH_TYPE.getName());
        verify(processScenario, never()).hasCompleted(ORDER_ROW_FULFILLMENT_PROCESS.getName());
        verify(processScenario).hasCompleted(END_MSG_ORDER_COMPLETED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }
}
