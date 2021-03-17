package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.*;
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
public class CheckRowCancellationPossibleTest {

    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Before
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testPassThruOnParcelShipmentRegular() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
        
        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(RowGateways.XOR_SHIPMENT_METHOD),
                util._N(RowEvents.PACKING_STARTED),
                util._N(RowEvents.TRACKING_ID_RECEIVED),
                util._N(RowEvents.ROW_SHIPPED),
                util._N(RowGateways.XOR_TOUR_STARTED),
                util._N(RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    public void testPassThruOnParcelShipmentExpress() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.EXPRESS));

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(RowGateways.XOR_SHIPMENT_METHOD),
                util._N(RowEvents.PACKING_STARTED),
                util._N(RowEvents.TRACKING_ID_RECEIVED),
                util._N(RowEvents.ROW_SHIPPED),
                util._N(RowGateways.XOR_TOUR_STARTED),
                util._N(RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    public void testPassThruOnParcelOwnDelivery() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TOUR_STARTED, orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(RowGateways.XOR_SHIPMENT_METHOD),
                util._N(RowEvents.TOUR_STARTED),
                util._N(RowGateways.XOR_TOUR_STARTED),
                util._N(RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    public void testPassThruOnParcelClickCollect() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.CLICK_COLLECT));

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);

        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.ROW_PREPARED, orderNumber);
        util.sendMessage(RowMessages.ROW_PICKED_UP, orderNumber);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(RowGateways.XOR_SHIPMENT_METHOD),
                util._N(RowEvents.ROW_PREPARED_FOR_PICKUP),
                util._N(RowEvents.ROW_PICKED_UP),
                util._N(RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED)
        );
        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).isEnded();
    }

    @Test
    public void testCancellationPossibleOnParcelShipmentAfterPackingStartedTrackingIdNOTSet() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder salesOrder = salesOrderUtil.createNewSalesOrder();
        processVariables.put(util._N(Variables.ORDER_NUMBER), salesOrder.getOrderNumber());
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));

        testProcess(processVariables, salesOrder.getOrderNumber());
    }

    @Test
    public void testCancellationPossibleOnParcelShipmentAfterPackingStartedTrackingIdIsSet() {
        final Map<String, Object> processVariables = new HashMap<>();
        SalesOrder salesOrder = salesOrderUtil.createNewSalesOrder();
        processVariables.put(util._N(Variables.ORDER_NUMBER), salesOrder.getOrderNumber());
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
        processVariables.put(util._N(RowVariables.TRACKING_ID_RECEIVED), false);

        testProcess(processVariables, salesOrder.getOrderNumber());
    }

    @Test
    public void testCancellationNotPossibleOnParcelShipmentAfterTrackingIdReceived() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
//        processVariables.put(util._N(ItemVariables.TRACKING_ID_RECEIVED), true);

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED, orderNumber);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(RowGateways.XOR_SHIPMENT_METHOD),
                util._N(RowEvents.PACKING_STARTED),
                util._N(RowEvents.TRACKING_ID_RECEIVED),
                util._N(RowActivities.CHECK_CANCELLATION_POSSIBLE),
                util._N(RowGateways.XOR_CANCELLATION_POSSIBLE)
        );

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassed(
                util._N(RowEvents.ORDER_ROW_CANCELLATION_NOT_HANDLED),
                util._N(BPMSalesOrderRowFulfillment.SUB_PROCESS_ORDER_ROW_CANCELLATION_SHIPMENT)
        );

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).isWaitingAt(util._N(RowEvents.ROW_SHIPPED));
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).hasPassed(
                util._N(RowEvents.TRACKING_ID_RECEIVED),
                util._N(RowEvents.ROW_SHIPPED)
        );
        BpmnAwareTests.assertThat(orderRowFulfillmentProcess).isEnded();

    }

    ProcessInstance testProcess(final Map<String, Object> processVariables, String orderNumber) {
        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED, orderNumber);

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(RowGateways.XOR_SHIPMENT_METHOD),
                util._N(RowEvents.PACKING_STARTED),
                util._N(RowEvents.MSG_ROW_CANCELLATION_RECEIVED),
                util._N(RowActivities.CHECK_CANCELLATION_POSSIBLE),
                util._N(RowGateways.XOR_CANCELLATION_POSSIBLE),
                util._N(BPMSalesOrderRowFulfillment.SUB_PROCESS_ORDER_ROW_CANCELLATION_SHIPMENT),
                util._N(RowEvents.ORDER_ROW_CANCELLATION_RECEIVED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(RowEvents.ORDER_ROW_CANCELLED),
                util._N(BPMSalesOrderRowFulfillment.SUB_PROCESS_HANDLE_ORDER_ROW_CANCELLATION)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).hasNotPassed(
//                util._N(ItemActivities.EVENT_TRACKING_ID_RECEIVED),
                util._N(RowEvents.ROW_SHIPPED)
        );

        BpmnAwareTests.assertThat(orderItemFulfillmentProcess).isEnded();

        return orderItemFulfillmentProcess;
    }
}
