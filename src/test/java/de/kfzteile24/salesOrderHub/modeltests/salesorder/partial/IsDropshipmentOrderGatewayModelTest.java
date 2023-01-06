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

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.PERSIST_DROPSHIPMENT_ORDER_ITEMS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_BRANCH_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_SOH_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAUSE_DROPSHIPMENT_ORDER_PROCESSING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IsDropshipmentOrderGateway model test")
@Slf4j(topic = "IsDropshipmentOrderGateway model test")
class IsDropshipmentOrderGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("IsDropshipmentOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckDropShipmentOrder. isDropshipmentOrder is true and" +
            " pauseDropshipmentOrderProcessing is true")
    void testIsDropshipmentOrderTrueAndPauseProcessing(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), true);
        processVariables.put(PAUSE_DROPSHIPMENT_ORDER_PROCESSING.getName(), true);

        when(processScenario.waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName()))
                .thenReturn(WAIT_SIGNAL_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                businessKey, processVariables);

        verify(processScenario, never()).hasStarted(THROW_MSG_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName());
        verify(processScenario).hasStarted(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        verify(processScenario).waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
    }

    @Test
    @Tags(@Tag("IsDropshipmentOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckDropShipmentOrder. isDropshipmentOrder is true and" +
            " pauseDropshipmentOrderProcessing is false")
    void testIsDropshipmentOrderTrueAndNotPauseProcessing(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), true);
        processVariables.put(PAUSE_DROPSHIPMENT_ORDER_PROCESSING.getName(), false);
        processVariables.put(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true);

        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.waitsAtEventBasedGateway(EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE.getName()))
                .thenReturn(RECEIVED_SIGNAL_EVENT_GATEWAY_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                businessKey, processVariables);

        verify(processScenario, never()).hasStarted(THROW_MSG_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName());
        verify(processScenario, never()).hasStarted(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName());


        verify(processScenario).hasCompleted(PERSIST_DROPSHIPMENT_ORDER_ITEMS.getName());
        verify(processScenario).hasCompleted(MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED.getName());
        verify(processScenario).waitsAtEventBasedGateway(EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE.getName());
    }

    @Test
    @Tags(@Tag("IsDropshipmentOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckDropShipmentOrder. isDropshipmentOrder is false")
    void testIsDropshipmentOrderFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_BRANCH_ORDER.getName(), true);
        processVariables.put(IS_SOH_ORDER.getName(), false);
        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), false);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_PAYMENT_SECURED.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(THROW_MSG_ORDER_CREATED.getName());
        verify(processScenario, never()).hasStarted(XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName());
    }
}
