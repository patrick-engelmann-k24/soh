package de.kfzteile24.salesOrderHub.modeltests;

import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderRowsCancellationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.CheckIsDropshipmentOrderListener;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.CheckProcessingDropshipmentOrderListener;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCancelledDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.listener.UpdateOrderEntityListener;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.START_MSG_ORDER_RECEIVED_FROM_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAUSE_DROPSHIPMENT_ORDER_PROCESSING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SAmple test class description")
@Slf4j(topic = "Sample Camunda model test")
@MockBean({
        OrderCreatedDelegate.class,
        CheckIsDropshipmentOrderListener.class,
        CheckProcessingDropshipmentOrderListener.class,
        UpdateOrderEntityListener.class,
        DropshipmentOrderRowsCancellationDelegate.class,
        OrderCancelledDelegate.class
})
class SampleModelTest extends AbstractWorkflowTest {

    @Test
    @Tags(@Tag("startByMessage"))
    @DisplayName("Start the entire process by message from process start")
    void testStartWholeProcessByMessageFromStart(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var processVariables = createProcessVariables(salesOrder);
        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), true);
        processVariables.put(PAUSE_DROPSHIPMENT_ORDER_PROCESSING.getName(), true);
        var businessKey = salesOrder.getOrderNumber();

        when(processScenario.waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName()))
                .thenReturn(EMPTY_SIGNAL_CATCH_EVENT_ACTION);

        scenario = startByMessage(ORDER_RECEIVED_ECP, businessKey, processVariables);

        assertThat(scenario.instance(processScenario)).variables().containsEntry(ORDER_NUMBER.getName(), salesOrder.getOrderNumber());

        verify(processScenario).hasStarted(START_MSG_ORDER_RECEIVED_FROM_ECP.getName());
        verify(processScenario).hasCompleted(START_MSG_ORDER_RECEIVED_FROM_ECP.getName());

        verify(processScenario).hasStarted(THROW_MSG_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_ORDER_CREATED.getName());

        verify(processScenario).hasStarted(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        verify(processScenario).waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
    }

    @Test
    @Tags(@Tag("startBeforeActivity"))
    @DisplayName("Start only a part of the given process before the given activity inside the process")
    void testStartPartOfProcessBeforeActivity(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var processVariables = createProcessVariables(salesOrder);
        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), true);
        processVariables.put(PAUSE_DROPSHIPMENT_ORDER_PROCESSING.getName(), true);
        var businessKey = salesOrder.getOrderNumber();

        when(processScenario.waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName()))
                .thenReturn(EMPTY_SIGNAL_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                businessKey, processVariables);

        assertThat(scenario.instance(processScenario)).variables().containsEntry(ORDER_NUMBER.getName(), salesOrder.getOrderNumber());

        verify(processScenario).hasStarted(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        verify(processScenario).waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
    }

    @Test
    @Tags(@Tag("startAfterActivity"))
    @DisplayName("Start only a part of the given process after the given activity inside the process")
    void testStartPartOfProcessAfterActivity(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var processVariables = createProcessVariables(salesOrder);
        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), true);
        processVariables.put(PAUSE_DROPSHIPMENT_ORDER_PROCESSING.getName(), true);
        var businessKey = salesOrder.getOrderNumber();

        when(processScenario.waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName()))
                .thenReturn(EMPTY_SIGNAL_CATCH_EVENT_ACTION);

        scenario = startAfterActivity(SALES_ORDER_PROCESS, THROW_MSG_ORDER_CREATED.getName(),
                businessKey, processVariables);

        assertThat(scenario.instance(processScenario)).variables().containsEntry(ORDER_NUMBER.getName(), salesOrder.getOrderNumber());

        verify(processScenario).hasStarted(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        verify(processScenario).waitsAtSignalIntermediateCatchEvent(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
    }

    @Test
    @Tags(@Tag("mockReceivingMessage"))
    @DisplayName("Start after 'eventThrowMsgCreatePurchaseOrder' and simulate receiving message on 'eventMsgDropShipmentOrderConfirmed'")
    void testMockReceivingMessageAtWaitingActivity(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        var businessKey = RandomStringUtils.randomNumeric(9);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName()))
                .thenReturn(action -> action.receive(Variables
                        .putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), false)
                        .putValue(ORDER_ROWS.getName(), List.of("sku-1", "sku-2"))
                ));

        scenario = startAfterActivity(SALES_ORDER_PROCESS, EVENT_THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName(),
                businessKey, Variables.createVariables());

        verify(processScenario).hasStarted(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName());
        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName());

        verify(processScenario, times(2)).hasStarted(DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName());
        verify(processScenario, times(2)).hasCompleted(DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName());

        verify(processScenario).hasStarted(END_MSG_ORDER_CANCELLED.getName());
        verify(processScenario).hasCompleted(END_MSG_ORDER_CANCELLED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }
}
