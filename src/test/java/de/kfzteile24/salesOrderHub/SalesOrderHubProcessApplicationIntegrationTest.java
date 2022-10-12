package de.kfzteile24.salesOrderHub;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.OrderRows;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesOrderHubProcessApplicationIntegrationTest extends AbstractIntegrationTest{

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private CamundaHelper camundaHelper;

    private SalesOrder testOrder;

    @BeforeEach
    public void setup() {
        super.setUp();
        testOrder = salesOrderUtil.createNewSalesOrder();
    }

    @Test
    public void startUpTest() {
        // context init test
        // test if the processEngine is configured correct and we can use it here.
        assertEquals("default", processEngine.getName());
        assertNotNull(runtimeService);
    }

    /**
     * Happy case test. Follow the process as intended
     */
    @Test
    public void salesOrderItemPassThruTest() {
        final String orderNumber = testOrder.getOrderNumber();
        final List<String> orderRows = util.getOrderRows(orderNumber, 5);

        ProcessInstance salesOrderProcessInstance =
                runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                        .processInstanceBusinessKey(orderNumber)
                        .setVariable(Variables.ORDER_NUMBER.getName(), orderNumber)
                        .setVariable(Variables.PAYMENT_TYPE.getName(), CREDIT_CARD.getName())
                        .setVariable(Variables.ORDER_VALID.getName(), true)
                        .setVariable(Variables.ORDER_ROWS.getName(), orderRows)
                        .setVariable(IS_ORDER_CANCELLED.getName(), false)
                        .setVariable(Variables.SHIPMENT_METHOD.getName(), REGULAR.getName())
                        .correlateWithResult().getProcessInstance();

        assertTrue(util.isProcessWaitingAtExpectedToken(salesOrderProcessInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));
        util.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), orderNumber);
        assertThat(salesOrderProcessInstance).isWaitingAt(Activities.ORDER_ROW_FULFILLMENT_PROCESS.getName());

        // send items thru
        util.sendMessage(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS.getName(), orderNumber);
        util.sendMessage(RowMessages.PACKING_STARTED.getName(), orderNumber);
        util.sendMessage(RowMessages.TRACKING_ID_RECEIVED.getName(), orderNumber);
        util.sendMessage(RowMessages.ROW_SHIPPED.getName(), orderNumber);

        assertThat(salesOrderProcessInstance).isEnded().hasPassed(Events.END_MSG_ORDER_COMPLETED.getName());
    }

    @Test
    public void noSubprocessesAreCreatedForOrderRowsWithShippingTypeNone() {
        final var salesOrder = salesOrderUtil.createPersistedSalesOrderV3(true, REGULAR, CREDIT_CARD, NEW);
        final var processInstance = camundaHelper.createOrderProcess(salesOrder, ORDER_RECEIVED_ECP);

        final var virtualOrderRowSkus = (List<String>) runtimeService.getVariable(processInstance.getProcessInstanceId(),
                Variables.VIRTUAL_ORDER_ROWS.getName());
        assertNotNull(virtualOrderRowSkus);
        assertEquals(1, virtualOrderRowSkus.size());

        final var expectedVirtualSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());
        Assertions.assertThat(virtualOrderRowSkus).isEqualTo(expectedVirtualSkus);

        final var orderRowSkus = runtimeService.getVariable(processInstance.getProcessInstanceId(),
                Variables.ORDER_ROWS.getName());

        final var expectedOrderRowSkus = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(orderRow -> !orderRow.getShippingType().equals(NONE.getName()))
                .map(OrderRows::getSku)
                .collect(Collectors.toList());
        Assertions.assertThat(orderRowSkus).isEqualTo(expectedOrderRowSkus);

        assertTrue(util.isProcessWaitingAtExpectedToken(processInstance, Events.MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        salesOrder.getLatestJson().getOrderRows()
                .forEach(orderRow -> Assertions.assertThat(
                        camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), orderRow.getSku()))
                        .isEqualTo(!NONE.getName().equals(orderRow.getShippingType())));
        assertFalse(camundaHelper.checkIfOrderRowProcessExists(salesOrder.getOrderNumber(), virtualOrderRowSkus.get(0)));
    }

}
