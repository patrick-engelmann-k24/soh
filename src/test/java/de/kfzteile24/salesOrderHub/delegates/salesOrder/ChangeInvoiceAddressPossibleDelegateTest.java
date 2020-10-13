package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.*;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.item.CheckItemCancellationPossible;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class ChangeInvoiceAddressTest {
    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    RuntimeService runtimeService;

    @Autowired
    RepositoryService repositoryService;

    @Autowired
    BpmUtil util;

    @Autowired
    SalesOrderService salesOrderService;

    private SalesOrder testOrder;


    @Before
    public void setUp() {
        init(processEngine);
        testOrder = salesOrderService.createOrder(util.getRandomOrderNumber());
    }

    @Test
    public void testChangeInvoiceAddressPossible() {
        final String orderId = testOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderId, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.PARCEL));
        processVariables.put(util._N(Variables.VAR_ORDER_ID), orderId);
        processVariables.put(util._N(Variables.VAR_PAYMENT_TYPE), "creditCard");
        processVariables.put(util._N(Variables.VAR_ORDER_VALID), true);
        processVariables.put(util._N(Variables.VAR_ORDER_ITEMS), orderItems);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), "parcel");

        final ProcessInstance orderProcess =
                runtimeService.createMessageCorrelation(util._N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                        .processInstanceBusinessKey(orderId)
                        .setVariables(processVariables)
                        .correlateWithResult().getProcessInstance();
        assertThat(orderProcess).isWaitingAt(util._N(Events.EVENT_MSG_ORDER_PAYMENT_SECURED));

        util.sendMessage(Messages.MSG_ORDER_INVOICE_ADDESS_CHANGE_RECEIVED);

        assertThat(orderProcess).hasPassedInOrder(
                util._N(ItemEvents.EVENT_START_ORDER_ITEM_FULFILLMENT_PROCESS),
                util._N(ItemEvents.EVENT_ITEM_TRANSMITTED_TO_LOGISTICS),
                util._N(ItemGateways.GW_XOR_SHIPMENT_METHOD),
                util._N(ItemEvents.EVENT_PACKING_STARTED),
                util._N(ItemEvents.EVENT_MSG_DELIVERY_ADDRESS_CHANGE),
                util._N(ItemActivities.ACTIVITY_CHECK_DELIVERY_ADDRESS_CHANGE_POSSIBLE),
                util._N(ItemGateways.GW_XOR_DELIVERY_ADRESS_CHANGE_POSSIBLE)
        );

        assertThat(orderProcess).hasPassed(
                util._N(BPMSalesOrderItemFullfilment.SUB_PROCESS_ORDER_ITEM_DELIVERY_ADDRES_CHANGE),
                util._N(ItemEvents.EVENT_DELIVERY_ADDRESS_NOT_CHANGED)
        );

        assertThat(orderProcess).hasNotPassed(
                util._N(ItemActivities.ACTIVITY_CHANGE_DELIVERY_ADDRESS)
        );

        assertThat(orderProcess).isWaitingAt(util._N(ItemEvents.EVENT_TRACKING_ID_RECEIVED));

        util.sendMessage(ItemMessages.MSG_TRACKING_ID_RECEIVED);
        util.sendMessage(ItemMessages.MSG_ITEM_DELIVERED);

        assertThat(orderProcess).isEnded();
    }
}
