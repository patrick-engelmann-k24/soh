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

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DropshipmentOrderTimer model test")
@Slf4j(topic = "DropshipmentOrderTimer model test")
class DropshipmentOrderTimerModelTest extends AbstractWorkflowTest {

    public static final String DEFER_PERIOD_SECONDS_25 = "PT25S";

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("DropshipmentOrderTimer"))
    @DisplayName("Start process before eventMsgDropShipmentOrderConfirmed." +
            " Wait until timer boundary event eventTimerDropShipmentOrderConfirmed is triggered twice")
    void testEventTimerDropShipmentOrderConfirmed(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true);

        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName()))
                .thenReturn(receiverTask -> receiverTask.defer(DEFER_PERIOD_SECONDS_25, receiverTask::receive));
        when(processScenario.runsCallActivity(DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS.getName()))
                .thenReturn(executeCallActivity());
        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);
        when(processScenario.waitsAtEventBasedGateway(EVENT_DROPSHIPMENT_ORDER_CANCEL_OR_COMPLETE.getName()))
                .thenReturn(RECEIVED_SIGNAL_EVENT_GATEWAY_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName());
        //TODO: newRelic metrics is required to be added to related delegate
//        verify(newRelicAwareTimerListener, times(2)).notify(any());
//        verify(metricsHelper, times(2))
//                .sendCustomEvent(eq(DROPSHIPMENT_ORDER_BOOKED_MISSING), argThat(
//                    eventAttributeMap -> {
//                        assertThat(eventAttributeMap).containsKeys("OrderNumber", "CreationDate", "DurationInMins");
//                        return true;
//                    }
//                ));
    }

    @Test
    @Tags(@Tag("DropshipmentOrderTimer"))
    @DisplayName("Start process before eventMsgDropShipmentOrderTrackingInformationReceived." +
            " Wait until timer boundary event eventTimerDropShipmentOrderTrackingInformationReceived is triggered twice")
    void eventTimerDropShipmentOrderTrackingInformationReceived(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName()))
                .thenReturn(receiverTask -> receiverTask.defer(DEFER_PERIOD_SECONDS_25, receiverTask::receive));

        scenario = startBeforeActivity(DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_PROCESS, EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED.getName());
        //TODO: newRelic metrics is required to be added to related delegate
//        verify(newRelicAwareTimerListener, times(2)).notify(any());
//        verify(metricsHelper, times(2))
//                .sendCustomEvent(eq(DROPSHIPMENT_SHIPMENT_CONFIRMED_MISSING), argThat(
//                        eventAttributeMap -> {
//                            assertThat(eventAttributeMap).containsKeys("OrderNumber", "CreationDate", "DurationInDays");
//                            return true;
//                        }
//                ));
    }
}
