package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.InvoiceService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.Payment.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_MARKETPLACE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_VALID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

class OrderControllerIntegrationTest extends AbstractIntegrationTest {

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
        super.setUp();
        testOrder = salesOrderUtil.createNewSalesOrder();
    }

    @Test
    void updatingTheInvoiceAddressSuccessfullyReturnsTheStatusOK() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();

        final BillingAddress address = BillingAddress.builder()
                                        .street1("Unit")
                                        .street2("Test")
                                        .city("Javaland")
                                        .zipCode("12345")
                                        .build();

        final var result = controller.updateBillingAddress(orderNumber, address);
        assertThat(result.getStatusCode()).isEqualTo(OK);
    }

    @Test
    void ifTheInvoiceAddressCannotBeUpdatedBecauseOfTheProcessStateTheStatusCONFLICTIsReturned() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        final BillingAddress address = BillingAddress.builder()
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
                .source(InvoiceSource.SOH)
                .build());

        final var result = controller.updateBillingAddress(orderNumber, address);
        assertThat(result.getStatusCode()).isEqualTo(CONFLICT);
    }

    @Test
    void cancellingAnOrderRowSuccessfullyReturnsTheStatusOK() {

        //This endpoint is not used anymore
        final var result = controller.cancelOrderRow("orderNumber", "orderRows");
        assertThat(result.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void cancellingAnOrderSuccessfullyReturnsTheStatusOK() {

        //This endpoint is not used anymore
        final var result = controller.cancelOrder("orderNumber");
        assertThat(result.getStatusCode()).isEqualTo(NOT_FOUND);
    }

    @Test
    void ifAnOrderCannotBeCancelledBecauseOfTheProcessStateTheStatusCONFLICTIsReturned() {

        //This endpoint is not used anymore
        final var result = controller.cancelOrder("orderNumber");
        assertThat(result.getStatusCode()).isEqualTo(NOT_FOUND);
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
