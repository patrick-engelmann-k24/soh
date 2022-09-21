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

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_BOOKED_MISSING;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_SHIPMENT_CONFIRMED_MISSING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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
        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName());
        verify(newRelicAwareTimerListener, times(2)).notify(any());
        verify(metricsHelper, times(2))
                .sendCustomEvent(eq(DROPSHIPMENT_ORDER_BOOKED_MISSING), argThat(
                    eventAttributeMap -> {
                        assertThat(eventAttributeMap).containsKeys("OrderNumber", "CreationDate", "DurationInMins");
                        return true;
                    }
                ));
    }

    @Test
    @Tags(@Tag("DropshipmentOrderTimer"))
    @DisplayName("Start process before eventMsgDropShipmentOrderTrackingInformationReceived." +
            " Wait until timer boundary event eventTimerDropShipmentOrderTrackingInformationReceived is triggered twice")
    void eventTimerDropShipmentOrderTrackingInformationReceived(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName()))
                .thenReturn(receiverTask -> receiverTask.defer(DEFER_PERIOD_SECONDS_25, receiverTask::receive));

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName());
        verify(newRelicAwareTimerListener, times(2)).notify(any());
        verify(metricsHelper, times(2))
                .sendCustomEvent(eq(DROPSHIPMENT_SHIPMENT_CONFIRMED_MISSING), argThat(
                        eventAttributeMap -> {
                            assertThat(eventAttributeMap).containsKeys("OrderNumber", "CreationDate", "DurationInDays");
                            return true;
                        }
                ));
    }
}
