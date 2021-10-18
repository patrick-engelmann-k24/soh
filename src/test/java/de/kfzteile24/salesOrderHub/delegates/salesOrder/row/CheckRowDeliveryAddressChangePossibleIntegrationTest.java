package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import lombok.SneakyThrows;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_ROW_FULFILLMENT_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.BPMSalesOrderRowFulfillment.SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowActivities.CHANGE_DELIVERY_ADDRESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowActivities.CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowGateways.XOR_CLICK_AND_COLLECT;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowGateways.XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowGateways.XOR_SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.DELIVERY_ADDRESS_CHANGE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.PACKING_STARTED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_PICKED_UP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_PREPARED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_SHIPPED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.TOUR_STARTED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.TRACKING_ID_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DELIVERY_ADDRESS_CHANGED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class CheckRowDeliveryAddressChangePossibleIntegrationTest {

    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private AuditLogUtil auditLogUtil;

    @Autowired
    private TimedPollingService pollingService;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testChangeAddressNotPossibleOnParcelShipmentAfterPackingStarted() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final Map<String, Object> processVariables = new HashMap<>();
        final String orderNumber = testOrder.getOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.REGULAR.getName());

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(PACKING_STARTED, orderNumber);
        util.sendMessage(DELIVERY_ADDRESS_CHANGE, orderNumber);

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                XOR_SHIPMENT_METHOD.getName(),
                RowEvents.PACKING_STARTED.getName(),
                RowEvents.MSG_DELIVERY_ADDRESS_CHANGE.getName(),
                CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE.getName(),
                XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE.getName(),
                RowEvents.DELIVERY_ADDRESS_NOT_CHANGED.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(CHANGE_DELIVERY_ADDRESS.getName());

        assertThat(orderItemFulfillmentProcess).isWaitingAt(RowEvents.TRACKING_ID_RECEIVED.getName());

        util.sendMessage(TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ROW_SHIPPED, orderNumber);

        assertThat(orderItemFulfillmentProcess).isEnded();

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogDoesNotExist(testOrder.getId(), DELIVERY_ADDRESS_CHANGED);
    }

    @Test
    @SneakyThrows(JsonProcessingException.class)
    public void testChangeAddressPossibleOnParcelShipment() {
        final Map<String, Object> processVariables = new HashMap<>();
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final String orderItemId = testOrder.getLatestJson().getOrderRows().get(0).getSku();

        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.REGULAR.getName());
        processVariables.put(RowVariables.ORDER_ROW_ID.getName(), orderItemId);

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);

        final Address address = Address.builder()
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        util.sendMessage(
                DELIVERY_ADDRESS_CHANGE,
                orderNumber,
                orderItemId,
                withVariables(RowVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), objectMapper.writeValueAsString(address))
        );

        verifyProcessFlowedUntilChangeDeliveryAddress(orderItemFulfillmentProcess);

        assertThat(orderItemFulfillmentProcess).hasPassed(
                SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE.getName(),
                RowEvents.DELIVERY_ADDRESS_CHANGED.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                RowEvents.DELIVERY_ADDRESS_NOT_CHANGED.getName()
        );

        assertThat(orderItemFulfillmentProcess).isWaitingAt(RowEvents.PACKING_STARTED.getName());
        util.sendMessage(PACKING_STARTED, orderNumber);

        assertThat(orderItemFulfillmentProcess).isWaitingAt(RowEvents.TRACKING_ID_RECEIVED.getName());

        util.sendMessage(TRACKING_ID_RECEIVED, orderNumber);
        util.sendMessage(ROW_SHIPPED, orderNumber);

        assertThat(orderItemFulfillmentProcess).isEnded();

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), DELIVERY_ADDRESS_CHANGED);
    }

    @Test
    public void testChangeAddressNotPossibleOnOwnDeliveryShipmentAfterTourStarted() {
        final Map<String, Object> processVariables = new HashMap<>();
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.DIRECT_DELIVERY.getName());

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(TOUR_STARTED, orderNumber);
        util.sendMessage(DELIVERY_ADDRESS_CHANGE, orderNumber);

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                XOR_SHIPMENT_METHOD.getName(),
                RowEvents.TOUR_STARTED.getName(),
                XOR_CLICK_AND_COLLECT.getName(),
                RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(CHANGE_DELIVERY_ADDRESS.getName());

        assertThat(orderItemFulfillmentProcess).isEnded();

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogDoesNotExist(testOrder.getId(), DELIVERY_ADDRESS_CHANGED);
    }

    @Test
    @SneakyThrows(JsonProcessingException.class)
    public void testChangeAddressPossibleOnOwnDeliveryShipment() {
        final Map<String, Object> processVariables = new HashMap<>();
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final String orderItemId = testOrder.getLatestJson().getOrderRows().get(0).getSku();

        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.DIRECT_DELIVERY.getName());
        processVariables.put(RowVariables.ORDER_ROW_ID.getName(), orderItemId);

        final Address address = Address.builder()
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);

        util.sendMessage(
                DELIVERY_ADDRESS_CHANGE,
                orderNumber,
                orderItemId,
                withVariables(RowVariables.DELIVERY_ADDRESS_CHANGE_REQUEST.getName(), objectMapper.writeValueAsString(address))
        );

        verifyProcessFlowedUntilChangeDeliveryAddress(orderItemFulfillmentProcess);

        assertThat(orderItemFulfillmentProcess).hasPassed(
                SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE.getName(),
                RowEvents.DELIVERY_ADDRESS_CHANGED.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(RowEvents.DELIVERY_ADDRESS_NOT_CHANGED.getName());

        assertThat(orderItemFulfillmentProcess).isWaitingAt(RowEvents.TOUR_STARTED.getName());
        util.sendMessage(TOUR_STARTED, orderNumber);
        util.sendMessage(ROW_SHIPPED, orderNumber);

        assertThat(orderItemFulfillmentProcess).isEnded();

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), DELIVERY_ADDRESS_CHANGED);
    }

    @Test
    public void testChangeAddressNotPossibleOnPickup() {
        final Map<String, Object> processVariables = new HashMap<>();
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(SHIPMENT_METHOD.getName(), ShipmentMethod.CLICK_COLLECT.getName());

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(DELIVERY_ADDRESS_CHANGE, orderNumber);

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                XOR_SHIPMENT_METHOD.getName(),
                RowEvents.MSG_DELIVERY_ADDRESS_CHANGE.getName(),
                CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE.getName(),
                XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE.getName(),
                RowEvents.DELIVERY_ADDRESS_NOT_CHANGED.getName()
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                CHANGE_DELIVERY_ADDRESS.getName()
        );

        assertThat(orderItemFulfillmentProcess).isWaitingAt(RowEvents.ROW_PREPARED_FOR_PICKUP.getName());

        util.sendMessage(ROW_PREPARED, orderNumber);
        util.sendMessage(ROW_PICKED_UP, orderNumber);

        assertThat(orderItemFulfillmentProcess).isEnded();

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogDoesNotExist(testOrder.getId(), DELIVERY_ADDRESS_CHANGED);
    }

    private void verifyProcessFlowedUntilChangeDeliveryAddress(ProcessInstance orderItemFulfillmentProcess) {
        final var processFlowedAsExpected = pollingService.pollWithDefaultTiming(() -> {
            assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                    RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                    RowEvents.ROW_TRANSMITTED_TO_LOGISTICS.getName(),
                    XOR_SHIPMENT_METHOD.getName(),
                    RowEvents.MSG_DELIVERY_ADDRESS_CHANGE.getName(),
                    CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE.getName(),
                    XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE.getName(),
                    CHANGE_DELIVERY_ADDRESS.getName()
            );

            return true;
        });
        assertTrue(processFlowedAsExpected);
    }
}
