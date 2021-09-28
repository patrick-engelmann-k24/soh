package de.kfzteile24.salesOrderHub.delegates.helper;

import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderRow;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class CamundaHelperTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private RuntimeService runtimeService;

    @InjectMocks
    private CamundaHelper camundaHelper;

    @Test
    public void theProcessVariablesAreCreatedCorrectly() {
        final var shipmentMethod = REGULAR;
        final var paymentType = CREDIT_CARD;
        final var customerType = NEW;
        final var salesOrder = createNewSalesOrderV3(true, shipmentMethod, paymentType, customerType);
        final var expectedVirtualOrderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());
        final var expectedOrderRowsSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> !orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());

        final var processVariables = camundaHelper.createProcessVariables(salesOrder);

        assertThat(processVariables.get(SHIPMENT_METHOD.getName())).isEqualTo(shipmentMethod.getName());
        assertThat(processVariables.get(ORDER_NUMBER.getName())).isEqualTo(salesOrder.getOrderNumber());
        assertThat(processVariables.get(PAYMENT_TYPE.getName())).isEqualTo(paymentType.getName());
        assertThat(processVariables.get(CUSTOMER_TYPE.getName())).isEqualTo(customerType.getType());
        assertThat(processVariables.get(ORDER_ROWS.getName())).isEqualTo(expectedOrderRowsSkus);
        assertThat(processVariables.get(VIRTUAL_ORDER_ROWS.getName())).isEqualTo(expectedVirtualOrderRowSkus);
    }

    @Test
    public void ifAnOrderHasNoVirtualItemsThenTheProcessVariablesDoNotContainTheVirtualOrderRowsVariable() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        final var processVariables = camundaHelper.createProcessVariables(salesOrder);
        assertThat(processVariables.containsKey(VIRTUAL_ORDER_ROWS.getName())).isFalse();
    }

    @Test
    public void recurringOrdersCreateTheCorrectCustomerTypeProcessVariable() {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, RECURRING);

        final var processVariables = camundaHelper.createProcessVariables(salesOrder);
        assertThat(processVariables.get(CUSTOMER_TYPE.getName())).isEqualTo(RECURRING.getType());
    }

    @Test
    public void multipleVirtualItemsAreHandledCorrectly() {
        final var salesOrder = createNewSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        Order orderJson = (Order) salesOrder.getOriginalOrder();
        var orderRows = new ArrayList<>(orderJson.getOrderRows());
        orderRows.add(createOrderRow("virtual-sku-2", NONE));
        orderJson.setOrderRows(orderRows);

        final var processVariables = camundaHelper.createProcessVariables(salesOrder);

        final var expectedVirtualOrderRowSkus = orderRows.stream()
                .filter(orderRow -> orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());

        assertThat(processVariables.get(VIRTUAL_ORDER_ROWS.getName())).isEqualTo(expectedVirtualOrderRowSkus);
    }
}