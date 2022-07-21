package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderRowsCancellationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.PublishDropshipmentTrackingInformationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.StoreDropshipmentInvoiceDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCancelledDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.OrderCompletedDelegate;
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
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROWS_CANCELLATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_PUBLISH_INVOICE_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.THROW_MSG_PUBLISH_TRACKING_INFORMATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("IsDropshipmentOrderCreatedGateway model test")
@Slf4j(topic = "IsDropshipmentOrderCreatedGateway model test")
@MockBean({
        PublishDropshipmentTrackingInformationDelegate.class,
        StoreDropshipmentInvoiceDelegate.class,
        PublishCoreSalesInvoiceCreatedReceivedDelegate.class,
        OrderCompletedDelegate.class,
        DropshipmentOrderRowsCancellationDelegate.class,
        OrderCancelledDelegate.class
})
class IsDropshipmentOrderCreatedGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("IsDropshipmentOrderCreatedGatewayTest"))
    @DisplayName("Start process before gwXORCheckDropShipmentOrderSuccessful. isDropshipmentOrderConfirmed is true")
    void testIsDropshipmentOrderCreatedTrue(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName()))
                .thenReturn(RECEIVED_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName());
        verify(processScenario).hasCompleted(EVENT_MSG_DROPSHIPMENT_ORDER_TRACKING_INFORMATION_RECEIVED.getName());
        verify(processScenario).hasCompleted(THROW_MSG_PUBLISH_TRACKING_INFORMATION.getName());
        verify(processScenario).hasCompleted(DROPSHIPMENT_ORDER_GENERATE_INVOICE.getName());
        verify(processScenario).hasCompleted(THROW_MSG_PUBLISH_INVOICE_DATA.getName());
        verify(processScenario).hasCompleted(END_MSG_ORDER_COMPLETED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }

    @Test
    @Tags(@Tag("IsDropshipmentOrderCreatedGatewayTest"))
    @DisplayName("Start process before gwXORCheckDropShipmentOrderSuccessful. isDropshipmentOrderConfirmed is false")
    void testIsDropshipmentOrderCreatedFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), false);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_DROPSHIPMENT_ORDER_SUCCESSFUL.getName(),
                businessKey, processVariables);

        verify(processScenario, times(3)).hasCompleted(DROPSHIPMENT_ORDER_ROWS_CANCELLATION.getName());
        verify(processScenario).hasCompleted(END_MSG_ORDER_CANCELLED.getName());

        assertThat(scenario.instance(processScenario)).isEnded();
    }
}
