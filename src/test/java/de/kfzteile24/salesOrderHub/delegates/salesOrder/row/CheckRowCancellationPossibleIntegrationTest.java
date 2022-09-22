package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowGateways;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import java.util.HashMap;
import java.util.Map;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckRowCancellationPossibleIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Test
    void testPassThroughOnParcelShipmentRegular() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), REGULAR.getName());
        
        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.TRACKING_ID_RECEIVED.getName(),
                RowEvents.ROW_SHIPPED.getName(),
                RowGateways.XOR_TOUR_STARTED.getName(),
                RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED.getName()
        );
        assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    void testPassThroughOnParcelShipmentExpress() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.EXPRESS.getName());

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.TRACKING_ID_RECEIVED.getName(),
                RowEvents.ROW_SHIPPED.getName(),
                RowGateways.XOR_TOUR_STARTED.getName(),
                RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED.getName()
        );
        assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    void testPassThroughOnParcelShipmentPriority() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.PRIORITY.getName());

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.TRACKING_ID_RECEIVED.getName(),
                RowEvents.ROW_SHIPPED.getName(),
                RowGateways.XOR_TOUR_STARTED.getName(),
                RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED.getName()
        );
        assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    void testPassThroughOnParcelOwnDelivery() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.DIRECT_DELIVERY.getName());

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TOUR_STARTED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.TOUR_STARTED.getName(),
                RowGateways.XOR_TOUR_STARTED.getName(),
                RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED.getName()
        );
        assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    void testPassThroughOnParcelClickCollect() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.CLICK_COLLECT.getName());

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.ROW_PREPARED, orderNumber);
        util.sendMessage(RowMessages.ROW_PICKED_UP, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.ROW_PREPARED_FOR_PICKUP.getName(),
                RowEvents.ROW_PICKED_UP.getName(),
                RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED.getName()
        );
        assertThat(orderRowFulfillmentProcess).isEnded();
    }
}
