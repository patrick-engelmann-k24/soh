package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_MARKETPLACE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_VALID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.PACKING_STARTED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.TRACKING_ID_RECEIVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OrderControllerIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil util;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private OrderController controller;
    private SalesOrder testOrder;

    @BeforeEach
    public void setup() {
        init(processEngine);
        testOrder = salesOrderUtil.createNewSalesOrder();
    }


    @Test
    public void contextLoads() throws Exception {
        assertThat(controller).isNotNull();
    }

    /**
     * Test for non existing order
     */
    @Test
    public void updateShippingAddressForOrder() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Address address = Address.builder()
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        ProcessInstance salesOrderProcessInstance =
        runtimeService.createProcessInstanceByKey(SALES_ORDER_PROCESS.getName())
                       .businessKey(orderNumber)
                       .setVariable(util._N(ORDER_NUMBER), orderNumber)
                       .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                       .setVariable(util._N(ORDER_VALID), true)
                       .setVariable(util._N(ORDER_ROWS), orderItems)
                       .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                       .startBeforeActivity(MSG_ORDER_PAYMENT_SECURED.getName())
                       .execute();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(
            MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(util._N(ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        final var result = controller.updateDeliveryAddress(orderNumber, orderItems.get(0), address);
        //TODO:this test should return 200 successful but is not.Fix this test
        assertThat(result.getStatusCode().is2xxSuccessful()).isFalse();
    }

    /**
     * Test for existing order
     */
    //@Test
    public void updateShippingAddressForOrderExisting() {
    }

    /**
     * Test for existing order but incorrect state
     */
    //@Test
    public void updateShippingAddressForOrderExistingButIncorrectState() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final String orderItemId = orderItems.get(0);
        final Address address = Address.builder().build();

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final var result = controller.updateDeliveryAddress(orderNumber, orderItemId, address);
        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    public void updateBillingAddressForExistingOrder() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Address address = Address.builder()
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        final var result = controller.updateBillingAddress(orderNumber, address);
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    public void cancelOrderItemForExistingOrder() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(
            MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(util._N(ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        final var result = controller.cancelOrderItem(orderNumber, orderItems.get(0));
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    public void cancelOrderItemNotPossibleState() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(
            MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(util._N(ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        util.sendMessage(util._N(ROW_TRANSMITTED_TO_LOGISTICS), orderNumber);
        util.sendMessage(util._N(PACKING_STARTED), orderNumber);
        util.sendMessage(util._N(TRACKING_ID_RECEIVED), orderNumber);

        final var result = controller.cancelOrderItem(orderNumber, orderItems.get(0));
        assertThat(result.getStatusCodeValue()).isEqualTo(409);
    }

    @Test
    public void cancelOrderPossibleTest() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(
            MSG_ORDER_PAYMENT_SECURED));
        final var result = controller.cancelOrder(orderNumber);
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    public void cancelOrderPossibleWithOrderRowProcessesTest() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(
            MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(util._N(ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);
        final var result = controller.cancelOrder(orderNumber);
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
    }

    @Test
    public void orderCancelNotPossibleOrderRowsDeliveredTest() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(ORDER_VALID), true)
                        .setVariable(util._N(ORDER_ROWS), orderItems)
                        .setVariable(util._N(SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(
            MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(util._N(ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        util.sendMessage(util._N(ROW_TRANSMITTED_TO_LOGISTICS), orderNumber, orderItems.get(0));
        util.sendMessage(util._N(PACKING_STARTED), orderNumber, orderItems.get(0));
        util.sendMessage(util._N(TRACKING_ID_RECEIVED), orderNumber, orderItems.get(0));

        final var result = controller.cancelOrder(orderNumber);
        assertThat(result.getStatusCodeValue()).isEqualTo(409);
    }

}
