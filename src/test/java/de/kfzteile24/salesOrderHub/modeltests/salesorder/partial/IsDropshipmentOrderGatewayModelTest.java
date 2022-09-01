package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderRowsCancellationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentOrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentTrackingInformationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.StoreDropshipmentInvoiceDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.CheckIsDropshipmentOrderListener;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.CheckProcessingDropshipmentOrderListener;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener.NewRelicAwareTimerListener;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCancelledDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCompletedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCreatedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.PublishCoreSalesInvoiceCreatedReceivedDelegate;
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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_GENERATE_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_DROPSHIPMENT_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_PUBLISH_INVOICE_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_PUBLISH_TRACKING_INFORMATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAUSE_DROPSHIPMENT_ORDER_PROCESSING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IsDropshipmentOrderGateway model test")
@Slf4j(topic = "IsDropshipmentOrderGateway model test")
@MockBean({
        CheckIsDropshipmentOrderListener.class,
        CheckProcessingDropshipmentOrderListener.class,
        DropshipmentOrderRowsCancellationDelegate.class,
        OrderCreatedDelegate.class,
        OrderCancelledDelegate.class,
        PublishDropshipmentOrderCreatedDelegate.class,
        NewRelicAwareTimerListener.class,
        PublishDropshipmentTrackingInformationDelegate.class,
        PublishCoreSalesInvoiceCreatedReceivedDelegate.class,
        StoreDropshipmentInvoiceDelegate.class,
        OrderCompletedDelegate.class
})
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
        when(processScenario.waitsAtReceiveTask(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName()))
                .thenReturn(RECEIVED_RECEIVER_TASK_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                businessKey, processVariables);

        verify(processScenario, never()).hasStarted(THROW_MSG_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName());
        verify(processScenario, never()).hasStarted(EVENT_SIGNAL_PAUSE_PROCESSING_DROPSHIPMENT_ORDER.getName());
        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_CONFIRMED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_DROPSHIPMENT_ORDER_CREATED.getName());
        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_PUBLISH_TRACKING_INFORMATION.getName());
        verify(processScenario).hasCompleted(DROPSHIPMENT_ORDER_GENERATE_INVOICE.getName());
        verify(processScenario).hasCompleted(THROW_MSG_PUBLISH_INVOICE_DATA.getName());
        verify(processScenario).hasCompleted(END_MSG_ORDER_COMPLETED.getName());
    }

    @Test
    @Tags(@Tag("IsDropshipmentOrderGatewayTest"))
    @DisplayName("Start process before gwXORCheckDropShipmentOrder. isDropshipmentOrder is false")
    void testIsDropshipmentOrderFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER.getName(), false);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_PAYMENT_SECURED.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(THROW_MSG_ORDER_CREATED.getName());
        verify(processScenario, never()).hasStarted(XOR_CHECK_PAUSE_PROCESSING_DROPSHIPMENT_ORDER_FLAG.getName());
    }
}
