package de.kfzteile24.salesOrderHub.delegates.salesOrder.item;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.*;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
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
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;

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

    @Autowired
    private Gson gson;

    @Before
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testChangeAddressNotPossibleOnParcelShipmentAfterPackingStarted() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(ItemMessages.DELIVERY_ADDRESS_CHANGE, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.PACKING_STARTED),
                util._N(ItemEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemActivities.CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.TRACKING_ID_RECEIVED));

        util.sendMessage(ItemMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ItemMessages.ITEM_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testChangeAddressPossibleOnParcelShipment() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);
        final String orderItemId = orderItems.get(0);

        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
        processVariables.put(util._N(ItemVariables.ORDER_ITEM_ID), orderItemId);

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);

        final Address address = Address.builder()
                .firstName("Max")
                .lastName("Mustermann")
                .street1("Unit")
                .street2("Test")
                .city("Javaland")
                .zipCode("12345")
                .build();

        util.sendMessage(
                ItemMessages.DELIVERY_ADDRESS_CHANGE,
                orderNumber,
                orderItemId,
                withVariables(ItemVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), gson.toJson(address))
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE),
                util._N(ItemActivities.CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.DELIVERY_ADDRESS_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.PACKING_STARTED));
        util.sendMessage(ItemMessages.PACKING_STARTED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.TRACKING_ID_RECEIVED));

        util.sendMessage(ItemMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ItemMessages.ITEM_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();

    }

    @Test
    public void testChangeAddressNotPossibleOnOwnDeliveryShipmentAfterTourStarted() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.TOUR_STARTED, orderNumber);
        util.sendMessage(ItemMessages.DELIVERY_ADDRESS_CHANGE, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.TOUR_STARTED),
                util._N(ItemGateways.XOR_CLICK_AND_COLLECT),
                util._N(ItemEvents.ORDER_ITEM_FULFILLMENT_PROCESS_FINISHED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemActivities.CHANGE_DELIVERY_ADDRESS)
        );


        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testChangeAddressPossibleOnOwnDeliveryShipment() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);
        final String orderItemId = orderItems.get(0);

        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));
        processVariables.put(util._N(ItemVariables.ORDER_ITEM_ID), orderItemId);

        final Address address = Address.builder()
                .firstName("Max")
                .lastName("Mustermann")
                .street1("Unit")
                .street2("Test")
                .city("Javaland")
                .zipCode("12345")
                .build();

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);

        util.sendMessage(
                ItemMessages.DELIVERY_ADDRESS_CHANGE,
                orderNumber,
                orderItemId,
                withVariables(ItemVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), gson.toJson(address))
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE),
                util._N(ItemActivities.CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.DELIVERY_ADDRESS_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.TOUR_STARTED));
        util.sendMessage(ItemMessages.TOUR_STARTED, orderNumber);
        util.sendMessage(ItemMessages.ITEM_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

    @Test
    public void testChangeAddressNotPossibleOnPickup() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.CLICK_COLLECT));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ITEM_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(ItemMessages.DELIVERY_ADDRESS_CHANGE, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(ItemEvents.START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(ItemActivities.CHANGE_DELIVERY_ADDRESS)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(ItemEvents.ITEM_PREPARED_FOR_PICKUP));

        util.sendMessage(ItemMessages.ITEM_PREPARED, orderNumber);
        util.sendMessage(ItemMessages.ITEM_PICKED_UP, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();
    }

}
