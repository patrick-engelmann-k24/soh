package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.*;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class CheckItemCancellationPossibleTest {
    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    BpmUtil util;

    @Autowired
    SalesOrderUtil salesOrderUtil;

    @Before
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testPassThruOnParcelShipmentRegular() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.SHIPMENT_REGULAR));
        
        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_PACKING_STARTED),
                util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED),
                util._N(ItemGateways.GW_XOR_TOUR_STARTED),
                util._N(ItemEvents.EVENT_ITEM_DELIVERED),
                util._N(ItemEvents.EVENT_ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testPassThruOnParcelShipmentExpress() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.SHIPMENT_EXPRESS));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_PACKING_STARTED),
                util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED),
                util._N(ItemGateways.GW_XOR_TOUR_STARTED),
                util._N(ItemEvents.EVENT_ITEM_DELIVERED),
                util._N(ItemEvents.EVENT_ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testPassThruOnParcelOwnDelivery() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_TOUR_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_TOUR_STARTED),
                util._N(ItemGateways.GW_XOR_TOUR_STARTED),
                util._N(ItemEvents.EVENT_ITEM_DELIVERED),
                util._N(ItemEvents.EVENT_ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testPassThruOnParcelClickCollect() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.CLICK_COLLECT));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ITEM_PREPARED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ITEM_PICKED_UP, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_ITEM_PREPARED_FOR_PICKUP),
                util._N(ItemEvents.EVENT_ITEM_PICKED_UP),
                util._N(ItemEvents.EVENT_ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testCancellationPossibleOnParcelShipmentAfterPackingStartedTrackingIdNOTSet() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder salesOrder = salesOrderUtil.createNewSalesOrder();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), salesOrder.getOrderNumber());
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.SHIPMENT_REGULAR));

        testProcess(processVariables, salesOrder.getOrderNumber());
    }

    @Test
    public void testCancellationPossibleOnParcelShipmentAfterPackingStartedTrackingIdIsSet() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder salesOrder = salesOrderUtil.createNewSalesOrder();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), salesOrder.getOrderNumber());
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.SHIPMENT_REGULAR));
        processVariables.put(util._N(ItemVariables.TRACKING_ID_RECEIVED), false);

        testProcess(processVariables, salesOrder.getOrderNumber());
    }

    @Test
    public void testCancellationNotPossibleOnParcelShipmentAfterTrackingIdReceived() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.SHIPMENT_REGULAR));
//        processVariables.put(util._N(ItemVariables.TRACKING_ID_RECEIVED), true);

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ORDER_ITEM_CANCELLATION_RECEIVED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_PACKING_STARTED),
                util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED),
                util._N(ItemGateways.GW_XOR_TOUR_STARTED),
                util._N(ItemActivities.ACTIVITY_CHECK_CANCELLATION_POSSIBLE),
                util._N(ItemGateways.GW_XOR_CANCELLATION_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(ItemEvents.EVENT_ORDER_ITEM_SHIPMENT_NOT_HANDLED),
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_CANCELLATION_SHIPMENT)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_ITEM_DELIVERED));
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED),
                util._N(ItemEvents.EVENT_ITEM_DELIVERED)
        );
        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();

    }

    ProcessInstance testProcess(final Map<String, Object> processVariables, String orderNumber) {
        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.MSG_ORDER_ITEM_CANCELLATION_RECEIVED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_PACKING_STARTED),
                util._N(ItemEvents.EVENT_MSG_SHIPMENT_CANCELLATION_RECEIVED),
                util._N(ItemActivities.ACTIVITY_CHECK_CANCELLATION_POSSIBLE),
                util._N(ItemGateways.GW_XOR_CANCELLATION_POSSIBLE),
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_CANCELLATION_SHIPMENT),
                util._N(ItemEvents.EVENT_ORDER_ITEM_CANCELLATION_RECEIVED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(ItemEvents.EVENT_ORDER_ITEM_CANCELLED),
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_HANDLE_ORDER_ITEM_CANCELLATION)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
//                util._N(ItemActivities.EVENT_TRACKING_ID_RECEIVED),
                util._N(ItemEvents.EVENT_ITEM_DELIVERED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();

        return orderItemFulfillmentProcess;
    }
}
