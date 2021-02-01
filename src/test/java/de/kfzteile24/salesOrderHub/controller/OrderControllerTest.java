package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class OrderControllerTest {

    @Autowired
    private OrderController controller;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    BpmUtil util;

    @Autowired
    SalesOrderUtil salesOrderUtil;

    private SalesOrder testOrder;

    @Before
    public void setup() {
        testOrder = salesOrderUtil.createNewSalesOrder();
    }


    @Test
    public void contextLoads() throws Exception {
        assertThat(controller).isNotNull();
    }

    /**
     * Test for non existing order
     */
    //@Test
    void updateShippingAddressForOrderNotExisting() {
        final String orderNumber = "1234567890";
        final String orderItemId = "1234567890";
        final Address address = Address.builder()
                .build();

        final var result = controller.updateDeliveryAddress(orderNumber, orderItemId, address);
        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
    }

    /**
     * Test for existing order
     */
    //@Test
    void updateShippingAddressForOrderExisting() {
    }

    /**
     * Test for existing order but incorrect state
     */
    //@Test
    void updateShippingAddressForOrderExistingButIncorrectState() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);

        final String orderItemId = orderItems.get(0);
        final Address address = Address.builder()
                .build();

        final var result = controller.updateDeliveryAddress(orderNumber, orderItemId, address);
        assertThat(result.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void updateBillingAddressForExistingOrder() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);

        final String orderItemId = orderItems.get(0);
        final Address address = Address.builder()
                .build();

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(util._N(Messages.ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(util._N(Variables.ORDER_NUMBER), orderNumber)
                        .setVariable(util._N(Variables.PAYMENT_TYPE), util._N(PaymentType.CREDIT_CARD))
                        .setVariable(util._N(Variables.ORDER_VALID), true)
                        .setVariable(util._N(Variables.ORDER_ITEMS), orderItems)
                        .setVariable(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR))
                        .correlateWithResult().getProcessInstance();
        BpmnAwareTests.assertThat(salesOrderProcessInstance).isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));

        final var result = controller.updateBillingAddress(orderNumber, address);
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
    }

}
