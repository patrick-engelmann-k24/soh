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
public class CheckItemChangePossibleTest {
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
    public void testChangeAddressNotPossibleOnParcelShipmentAfterPackingStarted() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderId = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderId);
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.PARCEL));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderId);
        util.sendMessage(ItemMessages.MSG_DELIVERY_ADDRESS_CHANGE, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_PACKING_STARTED),
                util._N(ItemEvents.EVENT_MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemActivities.ACTIVITY_CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED));

        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testChangeAddressPossibleOnParcelShipment() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        String orderId = testOrder.getOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderId);
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.PARCEL));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        util.sendMessage(ItemMessages.MSG_DELIVERY_ADDRESS_CHANGE, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE),
                util._N(ItemActivities.ACTIVITY_CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_PACKING_STARTED));
        util.sendMessage(ItemMessages.MSG_PACKING_STARTED, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED));

        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();

    }

    @Test
    public void testChangeAddressNotPossibleOnOwnDeliveryShipmentAfterTourStarted() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderId = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderId);
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        util.sendMessage(ItemMessages.MSG_TOUR_STARTED, orderId);
        util.sendMessage(ItemMessages.MSG_DELIVERY_ADDRESS_CHANGE, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_TOUR_STARTED),
                util._N(ItemEvents.EVENT_MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemActivities.ACTIVITY_CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_ITEM_DELIVERED));

        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testChangeAddressPossibleOnOwnDeliveryShipment() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        String orderId = testOrder.getOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderId);
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        util.sendMessage(ItemMessages.MSG_DELIVERY_ADDRESS_CHANGE, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE),
                util._N(ItemActivities.ACTIVITY_CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_TOUR_STARTED));
        util.sendMessage(ItemMessages.MSG_TOUR_STARTED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testChangeAddressNotPossibleOnPickup() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderId = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderId);
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.PICKUP));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.MSG_ITEM_TRANSMITTED, orderId);
        util.sendMessage(ItemMessages.MSG_DELIVERY_ADDRESS_CHANGE, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemActivities.ACTIVITY_CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.EVENT_ITEM_PREPARED_FOR_PICKUP));

        util.sendMessage(ItemMessages.MSG_ITEM_PREPARED, orderId);
        util.sendMessage(ItemMessages.MSG_ITEM_PICKED_UP, orderId);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

}
