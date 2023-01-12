package de.kfzteile24.salesOrderHub.controller;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.controller.impl.OrderController;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.dto.dropshipment.DropshipmentItemQuantity;
import de.kfzteile24.salesOrderHub.dto.dropshipment.DropshipmentOrderShipped;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_MARKETPLACE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_VALID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpStatus.CONFLICT;
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
    @Autowired
    private DropshipmentOrderRowService dropshipmentOrderRowService;
    @Autowired
    private DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

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
    void testShipItems() {
        var testOrder = salesOrderUtil.createNewSalesOrder();
        final String orderNumber = testOrder.getOrderNumber();
        var dropshipmentOrderRow1 = createDropshipmentOrderRow(orderNumber, "sku-1", 5, 2);
        var dropshipmentOrderRow2 = createDropshipmentOrderRow(orderNumber, "sku-2", 4, 3);
        var dropshipmentOrderRow3 = createDropshipmentOrderRow(orderNumber, "sku-3", 3, 1);
        DropshipmentOrderShipped dropshipmentOrderShipped = DropshipmentOrderShipped.builder()
                .orderNumber(orderNumber)
                .items(List.of(
                                DropshipmentItemQuantity.builder().sku("sku-1").quantity(3).build(),
                                DropshipmentItemQuantity.builder().sku("sku-2").quantity(1).build(),
                                DropshipmentItemQuantity.builder().sku("sku-3").quantity(2).build()
                        )
                )
                .build();
        doReturn(null).when(camundaHelper).correlateMessage(eq(DROPSHIPMENT_ORDER_FULLY_COMPLETED), eq(orderNumber));

        ResponseEntity<Object> responseEntity = controller.shipItems(dropshipmentOrderShipped);
        assertEquals(ResponseEntity.ok().build(), responseEntity);

        DropshipmentOrderRow saved1 = dropshipmentOrderRowService.getBySkuAndOrderNumber(
                dropshipmentOrderRow1.getSku(), dropshipmentOrderRow1.getOrderNumber()).orElseThrow();
        assertEquals(5, saved1.getQuantityShipped());
        DropshipmentOrderRow saved2 = dropshipmentOrderRowService.getBySkuAndOrderNumber(
                dropshipmentOrderRow2.getSku(), dropshipmentOrderRow2.getOrderNumber()).orElseThrow();
        assertEquals(4, saved2.getQuantityShipped());
        DropshipmentOrderRow saved3 = dropshipmentOrderRowService.getBySkuAndOrderNumber(
                dropshipmentOrderRow3.getSku(), dropshipmentOrderRow3.getOrderNumber()).orElseThrow();
        assertEquals(3, saved3.getQuantityShipped());

        DropshipmentInvoiceRow dropshipmentInvoiceRowSaved1 = dropshipmentInvoiceRowService.getBySkuAndOrderNumber(
                dropshipmentOrderRow1.getSku(), dropshipmentOrderRow1.getOrderNumber()).orElseThrow();
        assertEquals(3, dropshipmentInvoiceRowSaved1.getQuantity());
        DropshipmentInvoiceRow dropshipmentInvoiceRowSaved2 = dropshipmentInvoiceRowService.getBySkuAndOrderNumber(
                dropshipmentOrderRow2.getSku(), dropshipmentOrderRow2.getOrderNumber()).orElseThrow();
        assertEquals(1, dropshipmentInvoiceRowSaved2.getQuantity());
        DropshipmentInvoiceRow dropshipmentInvoiceRowSaved3 = dropshipmentInvoiceRowService.getBySkuAndOrderNumber(
                dropshipmentOrderRow3.getSku(), dropshipmentOrderRow3.getOrderNumber()).orElseThrow();
        assertEquals(2, dropshipmentInvoiceRowSaved3.getQuantity());

        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(testOrder.getOrderNumber()).orElseThrow();
        assertTrue(salesOrder.isShipped());
        verify(camundaHelper).correlateMessage(eq(DROPSHIPMENT_ORDER_FULLY_COMPLETED), eq(orderNumber));
    }

    private DropshipmentOrderRow createDropshipmentOrderRow(
            String orderNumber, String sku, Integer quantity, Integer quantityShipped) {
        return dropshipmentOrderRowService.save(DropshipmentOrderRow.builder()
                .sku(sku)
                .orderNumber(orderNumber)
                .quantity(quantity)
                .quantityShipped(quantityShipped)
                .build());
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
