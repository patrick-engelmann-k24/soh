package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCompletedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckOrderTypeDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.CheckPaymentTypeDelegate;
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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_BRANCH_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_BRANCH_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.POSITIVE_PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IsBranchOrderGateway model test")
@Slf4j(topic = "IsBranchOrderGateway model test")
@MockBean({
        CheckPlatformTypeDelegate.class,
        CheckOrderTypeDelegate.class,
        CheckPaymentTypeDelegate.class,
        OrderCompletedDelegate.class
})
class IsBranchOrderGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("IsBranchOrderGatewayTest"))
    @DisplayName("Start process before CheckOrderTypeDelegate. isBranchOrder is true")
    void testIsBranchOrderGatewayTestisBranchOrderTrue(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_BRANCH_ORDER.getName(), true);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_BRANCH_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(XOR_CHECK_BRANCH_TYPE.getName());
        verify(processScenario, never()).hasCompleted(XOR_CHECK_PAYMENT_TYPE.getName());
        verify(processScenario, never()).hasCompleted(ORDER_ROW_FULFILLMENT_PROCESS.getName());
        verify(processScenario).hasCompleted(END_MSG_ORDER_COMPLETED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }

    @Test
    @Tags(@Tag("IsBranchOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckBranchType. isBranchOrder is false")
    void testIsBranchOrderGatewayTestisBranchOrderFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_BRANCH_ORDER.getName(), false);
        processVariables.put(POSITIVE_PAYMENT_TYPE.getName(), true);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_PAYMENT_SECURED.getName()))
                .thenReturn(RECEIVED_MESSAGE_CATCH_EVENT_ACTION);
        when(processScenario.waitsAtMessageIntermediateCatchEvent(ROW_TRANSMITTED_TO_LOGISTICS.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);
        when(processScenario.runsCallActivity(ORDER_ROW_FULFILLMENT_PROCESS.getName()))
                .thenReturn(executeCallActivity());

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_BRANCH_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(XOR_CHECK_BRANCH_TYPE.getName());
        verify(processScenario).hasCompleted(XOR_CHECK_PAYMENT_TYPE.getName());
        verify(processScenario, times(3)).waitsAtMockedCallActivity(ORDER_ROW_FULFILLMENT_PROCESS.getName());
        verify(processScenario, times(3)).waitsAtMessageIntermediateCatchEvent(ROW_TRANSMITTED_TO_LOGISTICS.getName());
    }
}
