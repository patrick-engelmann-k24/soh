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
import java.util.List;
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

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testChangeAddressNotPossibleOnParcelShipmentAfterPackingStarted() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final Map<String, Object> processVariables = new HashMap<>();
        final String orderNumber = testOrder.getOrderNumber();
        processVariables.put(util._N(ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(PACKING_STARTED, orderNumber);
        util.sendMessage(DELIVERY_ADDRESS_CHANGE, orderNumber);

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(XOR_SHIPMENT_METHOD),
                util._N(RowEvents.PACKING_STARTED),
                util._N(RowEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE),
                util._N(RowEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(CHANGE_DELIVERY_ADDRESS)
        );

        assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(RowEvents.TRACKING_ID_RECEIVED));

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
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);
        final String orderItemId = orderItems.get(0);

        processVariables.put(util._N(ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
        processVariables.put(util._N(RowVariables.ORDER_ROW_ID), orderItemId);

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

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(XOR_SHIPMENT_METHOD),
                util._N(RowEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE),
                util._N(CHANGE_DELIVERY_ADDRESS)
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE),
                util._N(RowEvents.DELIVERY_ADDRESS_CHANGED)
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(RowEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(RowEvents.PACKING_STARTED));
        util.sendMessage(PACKING_STARTED, orderNumber);

        assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(RowEvents.TRACKING_ID_RECEIVED));

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
        processVariables.put(util._N(ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(TOUR_STARTED, orderNumber);
        util.sendMessage(DELIVERY_ADDRESS_CHANGE, orderNumber);

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(XOR_SHIPMENT_METHOD),
                util._N(RowEvents.TOUR_STARTED),
                util._N(XOR_CLICK_AND_COLLECT),
                util._N(RowEvents.ORDER_ROW_FULFILLMENT_PROCESS_FINISHED)
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(CHANGE_DELIVERY_ADDRESS)
        );

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
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);
        final String orderItemId = orderItems.get(0);

        processVariables.put(util._N(ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.OWN_DELIVERY));
        processVariables.put(util._N(RowVariables.ORDER_ROW_ID), orderItemId);

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

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(XOR_SHIPMENT_METHOD),
                util._N(RowEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE),
                util._N(CHANGE_DELIVERY_ADDRESS)
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE),
                util._N(RowEvents.DELIVERY_ADDRESS_CHANGED)
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(RowEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(RowEvents.TOUR_STARTED));
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
        processVariables.put(util._N(ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.CLICK_COLLECT));

        final ProcessInstance orderItemFulfillmentProcess = runtimeService.startProcessInstanceByKey(
                SALES_ORDER_ROW_FULFILLMENT_PROCESS.getName(),
                processVariables);
        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(DELIVERY_ADDRESS_CHANGE, orderNumber);

        assertThat(orderItemFulfillmentProcess).hasPassedInOrder(
                util._N(RowEvents.START_ORDER_ROW_FULFILLMENT_PROCESS),
                util._N(RowEvents.ROW_TRANSMITTED_TO_LOGISTICS),
                util._N(XOR_SHIPMENT_METHOD),
                util._N(RowEvents.MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        assertThat(orderItemFulfillmentProcess).hasPassed(
                util._N(SUB_PROCESS_ORDER_ROW_DELIVERY_ADDRESS_CHANGE),
                util._N(RowEvents.DELIVERY_ADDRESS_NOT_CHANGED)
        );

        assertThat(orderItemFulfillmentProcess).hasNotPassed(
                util._N(CHANGE_DELIVERY_ADDRESS)
        );

        assertThat(orderItemFulfillmentProcess).isWaitingAt(util._N(RowEvents.ROW_PREPARED_FOR_PICKUP));

        util.sendMessage(ROW_PREPARED, orderNumber);
        util.sendMessage(ROW_PICKED_UP, orderNumber);

        assertThat(orderItemFulfillmentProcess).isEnded();

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogDoesNotExist(testOrder.getId(), DELIVERY_ADDRESS_CHANGED);
    }

}
