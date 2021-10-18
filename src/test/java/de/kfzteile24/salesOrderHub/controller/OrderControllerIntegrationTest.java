package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.InvoiceService;
import de.kfzteile24.soh.order.dto.Address;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_MARKETPLACE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_VALID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.PACKING_STARTED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.ROW_TRANSMITTED_TO_LOGISTICS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages.TRACKING_ID_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

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
    @Autowired
    private InvoiceService invoiceService;

    private SalesOrder testOrder;

    @BeforeEach
    public void setup() {
        init(processEngine);
        testOrder = salesOrderUtil.createNewSalesOrder();
    }

    @Test
    public void updateDeliveryAddressForOrder() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = testOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(toList());

        final Address address = Address.builder()
                                        .firstName("Max")
                                        .lastName("Mustermann")
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        final var processInstance = createProcessInstance(orderNumber, orderItems);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, orderNumber);

        final var result = controller.updateDeliveryAddress(orderNumber, orderItems.get(0), address);
//        assertThat(result.getStatusCode()).isEqualTo(OK);
    }

    @Test
    public void ifTheDeliveryAddressOfAnOrderRowCannotBeUpdatedBecauseOfTheProcessStateTheStatus_CONFLICT_IsReturned() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final String orderItemId = orderItems.get(0);
        final Address address = Address.builder().build();

        final ProcessInstance processInstance = createProcessInstance(orderNumber, orderItems);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, orderNumber);

        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(PACKING_STARTED, orderNumber);
        util.sendMessage(TRACKING_ID_RECEIVED, orderNumber);

        final var result = controller.updateDeliveryAddress(orderNumber, orderItemId, address);
        assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    }

    @Test
    public void updatingTheInvoiceAddressSuccessfullyReturnsTheStatusOK() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final Address address = Address.builder()
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        final ProcessInstance processInstance = createProcessInstance(orderNumber, orderRows);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));

        final var result = controller.updateBillingAddress(orderNumber, address);
        assertThat(result.getStatusCode()).isEqualTo(OK);
    }

    @Test
    public void ifTheInvoiceAddressCannotBeUpdatedBecauseOfTheProcessStateTheStatus_CONFLICT_IsReturned() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final Address address = Address.builder()
                .street1("Unit")
                .street2("Test")
                .city("Javaland")
                .zipCode("12345")
                .build();

        final ProcessInstance processInstance = createProcessInstance(orderNumber, orderRows);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));
        invoiceService.addSalesOrderToInvoice(testOrder, SalesOrderInvoice.builder()
                .orderNumber(orderNumber)
                .invoiceNumber("444")
                .build());

        final var result = controller.updateBillingAddress(orderNumber, address);
        assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    }

    @Test
    public void cancellingAnOrderRowSuccessfullyReturnsTheStatusOK() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final var processInstance = createProcessInstance(orderNumber, orderRows);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, orderNumber);

        final var result = controller.cancelOrderRow(orderNumber, orderRows.get(0));
        assertThat(result.getStatusCode()).isEqualTo(OK);
    }

    @Test
    public void ifAnOrderRowCannotBeCancelledBecauseOfTheProcessStateTheStatus_CONFLICT_IsReturned() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final var processInstance = createProcessInstance(orderNumber, orderRows);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, orderNumber);

        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber);
        util.sendMessage(PACKING_STARTED, orderNumber);
        util.sendMessage(TRACKING_ID_RECEIVED, orderNumber);

        final var result = controller.cancelOrderRow(orderNumber, orderRows.get(0));
        assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    }

    @Test
    public void cancellingAnOrderSuccessfullyReturnsTheStatusOK() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final var processInstance = createProcessInstance(orderNumber, orderRows);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));

        final var result = controller.cancelOrder(orderNumber);
        assertThat(result.getStatusCode()).isEqualTo(OK);
    }

    @Test
    public void ifAnOrderCannotBeCancelledBecauseOfTheProcessStateTheStatus_CONFLICT_IsReturned() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final var processInstance = createProcessInstance(orderNumber, orderRows);
        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(ORDER_RECEIVED_PAYMENT_SECURED, orderNumber);

        util.sendMessage(ROW_TRANSMITTED_TO_LOGISTICS, orderNumber, orderRows.get(0));
        util.sendMessage(PACKING_STARTED, orderNumber, orderRows.get(0));
        util.sendMessage(TRACKING_ID_RECEIVED, orderNumber, orderRows.get(0));

        final var result = controller.cancelOrder(orderNumber);
        assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    }

    private ProcessInstance createProcessInstance(String orderNumber, List<String> orderItems) {
        return runtimeService.createMessageCorrelation(ORDER_RECEIVED_MARKETPLACE.getName())
                .processInstanceBusinessKey(orderNumber)
                .setVariable(ORDER_NUMBER.getName(), orderNumber)
                .setVariable(PAYMENT_TYPE.getName(), CREDIT_CARD.getName())
                .setVariable(ORDER_VALID.getName(), true)
                .setVariable(ORDER_ROWS.getName(), orderItems)
                .setVariable(SHIPMENT_METHOD.getName(), REGULAR.getName())
                .correlateWithResult().getProcessInstance();
    }

}
