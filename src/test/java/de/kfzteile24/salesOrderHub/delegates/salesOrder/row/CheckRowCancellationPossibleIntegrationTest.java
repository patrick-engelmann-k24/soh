package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.BPMSalesOrderRowFulfillment;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowActivities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowGateways;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.ORDER_ROW_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables.TRACKING_ID_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class
)
public class CheckRowCancellationPossibleIntegrationTest {

    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testPassThruOnParcelShipmentRegular() {
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
    public void testPassThruOnParcelShipmentExpress() {
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
    public void testPassThruOnParcelOwnDelivery() {
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
    public void testPassThruOnParcelClickCollect() {
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

    @Test
    public void testCancellationPossibleOnParcelShipmentAfterPackingStartedTrackingIdNOTSet() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var skuToCancel = salesOrder.getLatestJson().getOrderRows().get(0).getSku();

        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), salesOrder.getOrderNumber(),
                SHIPMENT_METHOD.getName(), REGULAR.getName(),
                ORDER_ROW_ID.getName(), skuToCancel
        );

        ProcessInstance processInstance = testProcess(processVariables, salesOrder.getLatestJson(), skuToCancel);
        assertThat(processInstance).isEnded();
    }

    @Test
    public void testCancellationPossibleOnParcelShipmentAfterPackingStartedTrackingIdIsSet() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        final var skuToCancel = salesOrder.getLatestJson().getOrderRows().get(0).getSku();

        final Map<String, Object> processVariables = Map.of(
            ORDER_NUMBER.getName(), salesOrder.getOrderNumber(),
            SHIPMENT_METHOD.getName(), REGULAR.getName(),
            ORDER_ROW_ID.getName(), skuToCancel,
            TRACKING_ID_RECEIVED.getName(), false
        );

        ProcessInstance processInstance = testProcess(processVariables, salesOrder.getLatestJson(), skuToCancel);
        assertThat(processInstance).isEnded();
    }

    @Test
    public void testCancellationNotPossibleOnParcelShipmentAfterTrackingIdReceived() {
        final Map<String, Object> processVariables = new HashMap<>();
        String orderNumber = util.getRandomOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), REGULAR.getName());
//        processVariables.put(util._N(ItemVariables.TRACKING_ID_RECEIVED), true);

        final ProcessInstance orderRowFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.TRACKING_ID_RECEIVED.getName(),
                RowActivities.CHECK_CANCELLATION_POSSIBLE.getName(),
                RowGateways.XOR_CANCELLATION_POSSIBLE.getName()
        );

        assertThat(orderRowFulfillmentProcess).hasPassed(
                RowEvents.ORDER_ROW_CANCELLATION_NOT_HANDLED.getName(),
                BPMSalesOrderRowFulfillment.SUB_PROCESS_ORDER_ROW_CANCELLATION_SHIPMENT.getName()
        );

        assertThat(orderRowFulfillmentProcess).isWaitingAt(RowEvents.ROW_SHIPPED.getName());
        util.sendMessage(RowMessages.ROW_SHIPPED, orderNumber);

        assertThat(orderRowFulfillmentProcess).hasPassed(
                RowEvents.TRACKING_ID_RECEIVED.getName(),
                RowEvents.ROW_SHIPPED.getName()
        );
        assertThat(orderRowFulfillmentProcess).isEnded();

    }

    private ProcessInstance testProcess(final Map<String, Object> processVariables, Order order, String skuToCancel) {
        final var orderNumber = order.getOrderHeader().getOrderNumber();
        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED, orderNumber);
        util.sendMessage(RowMessages.ORDER_ROW_CANCELLATION_RECEIVED, orderNumber, skuToCancel);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                RowGateways.XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.MSG_ROW_CANCELLATION_RECEIVED.getName(),
                RowActivities.CHECK_CANCELLATION_POSSIBLE.getName(),
                RowGateways.XOR_CANCELLATION_POSSIBLE.getName(),
                BPMSalesOrderRowFulfillment.SUB_PROCESS_ORDER_ROW_CANCELLATION_SHIPMENT.getName(),
                RowEvents.ORDER_ROW_CANCELLATION_RECEIVED.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                RowEvents.ORDER_ROW_CANCELLED.getName(),
                BPMSalesOrderRowFulfillment.SUB_PROCESS_HANDLE_ORDER_ROW_CANCELLATION.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                RowEvents.ROW_SHIPPED.getName()
        );

        return orderItemFulfillmentProcess;
    }
}
