package de.kfzteile24.salesOrderHub.modeltests.dropshipment;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_CREATE_UPDATE_SHIPMENT_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_PUBLISH_TRACKING_INFORMATION;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_DROPSHIPMENT_SHIPMENT;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_PUBLISH_DROPSHIPMENT_ITEM_SHIPMENT_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ITEMS_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_FULLY_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("DropshipmentShipment process model test")
@Slf4j(topic = "DropshipmentShipment process model test")
class DropshipmentShipmentModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }


    @Test
    @Tags(@Tag("DropshipmentShipmentProcessTest"))
    @DisplayName("Start DropshipmentShipmentProcess process before eventMsgDropShipmentOrderConfirmed. shipped is false," +
            " itemFullyShipped is true")
    void testDropshipmentOrderRowShipmentConfirmedSubprocess(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(ORDER_FULLY_SHIPPED.getName(), false);
        processVariables.put(ITEMS_FULLY_SHIPPED.getName(), true);

        scenario = startByMessage(DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED, businessKey, processVariables);

        verify(processScenario).hasCompleted(DROPSHIPMENT_CREATE_UPDATE_SHIPMENT_DATA.getName());
        verify(processScenario).hasCompleted(EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_PUBLISH_TRACKING_INFORMATION.getName());
        verify(processScenario).hasCompleted(END_MSG_PUBLISH_DROPSHIPMENT_ITEM_SHIPMENT_COMPLETED.getName());
        verify(processScenario, never()).hasCompleted(END_DROPSHIPMENT_SHIPMENT.getName());
    }

    @Test
    @Tags(@Tag("DropshipmentShipmentProcessTest"))
    @DisplayName("Start DropshipmentShipmentProcess process before eventMsgDropShipmentOrderConfirmed. shipped is false," +
            " itemFullyShipped is false")
    void testDropshipmentOrderRowShipmentConfirmedSubprocessf(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(ORDER_FULLY_SHIPPED.getName(), false);
        processVariables.put(ITEMS_FULLY_SHIPPED.getName(), false);

        scenario = startByMessage(DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED, businessKey, processVariables);

        verify(processScenario).hasCompleted(DROPSHIPMENT_CREATE_UPDATE_SHIPMENT_DATA.getName());
        verify(processScenario).hasCompleted(EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_PUBLISH_TRACKING_INFORMATION.getName());
        verify(processScenario, never()).hasCompleted(END_MSG_PUBLISH_DROPSHIPMENT_ITEM_SHIPMENT_COMPLETED.getName());
        verify(processScenario).hasCompleted(END_DROPSHIPMENT_SHIPMENT.getName());
    }

    @Test
    @Tags(@Tag("DropshipmentShipmentProcessTest"))
    @DisplayName("Start DropshipmentShipmentProcess process before eventMsgDropShipmentOrderConfirmed. shipped is true")
    void testDropshipmentOrderRowShipmentConfirmedSubprocessfg(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(ORDER_FULLY_SHIPPED.getName(), true);

        scenario = startByMessage(DROPSHIPMENT_SHIPMENT_CONFIRMATION_RECEIVED, businessKey, processVariables);

        verify(processScenario, never()).hasCompleted(DROPSHIPMENT_CREATE_UPDATE_SHIPMENT_DATA.getName());
        verify(processScenario).hasCompleted(EVENT_END_MSG_DROPSHIPMENT_ORDER_ROW_PUBLISH_TRACKING_INFORMATION.getName());
        verify(processScenario, never()).hasCompleted(END_MSG_PUBLISH_DROPSHIPMENT_ITEM_SHIPMENT_COMPLETED.getName());
        verify(processScenario).hasCompleted(END_DROPSHIPMENT_SHIPMENT.getName());
    }

}