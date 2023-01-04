package de.kfzteile24.salesOrderHub.delegates.helper;

import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.VOUCHER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3WithPlatform;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderRow;
import static de.kfzteile24.soh.order.dto.Platform.SOH;
import static org.assertj.core.api.Assertions.assertThat;

class CamundaHelperTest {

    @Test
    void theProcessVariablesAreCreatedCorrectly() {
        final var shipmentMethod = REGULAR;
        final var paymentType = CREDIT_CARD;
        final var platformType = SOH;
        final var customerType = NEW;
        final var salesOrder = createNewSalesOrderV3WithPlatform(true, shipmentMethod, paymentType, platformType, customerType);
        final var expectedVirtualOrderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());
        final var expectedOrderRowsSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> !orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());

        final var processVariables = SalesOrderProcessService.createProcessVariables(salesOrder);

        assertThat(processVariables.get(SHIPMENT_METHOD.getName())).isEqualTo(shipmentMethod.getName());
        assertThat(processVariables.get(ORDER_NUMBER.getName())).isEqualTo(salesOrder.getOrderNumber());
        assertThat(processVariables.get(PAYMENT_TYPE.getName())).isEqualTo(paymentType.getName());
        assertThat(processVariables.get(PLATFORM_TYPE.getName())).isEqualTo(platformType.name());
        assertThat(processVariables.get(CUSTOMER_TYPE.getName())).isEqualTo(customerType.getType());
        assertThat(processVariables.get(ORDER_ROWS.getName())).isEqualTo(expectedOrderRowsSkus);
        assertThat(processVariables.get(VIRTUAL_ORDER_ROWS.getName())).isEqualTo(expectedVirtualOrderRowSkus);
    }

    @Test
    void ifAnOrderHasNoVirtualItemsThenTheProcessVariablesDoNotContainTheVirtualOrderRowsVariable() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        final var processVariables = SalesOrderProcessService.createProcessVariables(salesOrder);
        assertThat(processVariables.containsKey(VIRTUAL_ORDER_ROWS.getName())).isFalse();
    }

    @Test
    void recurringOrdersCreateTheCorrectCustomerTypeProcessVariable() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, RECURRING);

        final var processVariables = SalesOrderProcessService.createProcessVariables(salesOrder);
        assertThat(processVariables.get(CUSTOMER_TYPE.getName())).isEqualTo(RECURRING.getType());
    }

    @Test
    void multipleVirtualItemsAreHandledCorrectly() {
        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        Order orderJson = (Order) salesOrder.getOriginalOrder();
        var orderRows = new ArrayList<>(orderJson.getOrderRows());
        orderRows.add(createOrderRow("virtual-sku-2", NONE));
        orderJson.setOrderRows(orderRows);

        final var processVariables = SalesOrderProcessService.createProcessVariables(salesOrder);

        final var expectedVirtualOrderRowSkus = orderRows.stream()
                .filter(orderRow -> orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());

        assertThat(processVariables.get(VIRTUAL_ORDER_ROWS.getName())).isEqualTo(expectedVirtualOrderRowSkus);
    }

    @Test
    void thePaymentTypeVoucherIsNotAddedToTheProcessVariables() {
        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        Order orderJson = (Order) salesOrder.getOriginalOrder();
        final var payments = List.of(
                Payments.builder().type(VOUCHER.getName()).build(),
                orderJson.getOrderHeader().getPayments().get(0)
        );
        orderJson.getOrderHeader().setPayments(payments);

        final var processVariables = SalesOrderProcessService.createProcessVariables(salesOrder);
        assertThat(processVariables.get(PAYMENT_TYPE.getName())).isEqualTo(CREDIT_CARD.getName());
    }
}