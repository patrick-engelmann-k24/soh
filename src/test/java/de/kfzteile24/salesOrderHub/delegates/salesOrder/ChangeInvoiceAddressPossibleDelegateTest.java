package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.*;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
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
public class ChangeInvoiceAddressPossibleDelegateTest {
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

    @Autowired
    SalesOrderInvoiceRepository invoiceRepository;

    @Before
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testChangeInvoiceAddressPossible() {
        final SalesOrder testOrder = getSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderId = testOrder.getOrderNumber();

        assertThat(orderProcess).isWaitingAt(util._N(Events.EVENT_MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.MSG_ORDER_INVOICE_ADDESS_CHANGE_RECEIVED);

        assertThat(orderProcess).hasPassedInOrder(
                util._N(Events.EVENT_START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED),
                util._N(Activities.ACTIVITY_CHANGE_INVOICE_ADDRESS_POSSIBLE),
                util._N(Gateways.GW_XOR_INVOICE_EXIST)
        );
        assertThat(orderProcess).hasPassed(
                util._N(Activities.ACTIVITY_CHANGE_INVOICE_ADDRESS),
                util._N(Activities.ACTIVITY_SUB_PROCESS_INVOICE_ADDRESS_CHANGE)
        );

        assertThat(orderProcess).hasNotPassed(util._N(Events.EVENT_INVOICE_ADDRESS_NOT_CHANGED));

        finishOrderProcess(orderProcess, orderId);
    }

    @Test
    public void testChangeInvoiceAddressPossibleNotPossible() {
        final SalesOrder testOrder = getSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderId = testOrder.getOrderNumber();

        final SalesOrderInvoice orderInvoice = SalesOrderInvoice.builder()
                .salesOrder(testOrder)
                .invoiceNumber(util.getRandomOrderNumber())
                .build();
        invoiceRepository.save(orderInvoice);

        assertThat(orderProcess).isWaitingAt(util._N(Events.EVENT_MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.MSG_ORDER_INVOICE_ADDESS_CHANGE_RECEIVED);

        assertThat(orderProcess).hasPassedInOrder(
                util._N(Events.EVENT_START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED),
                util._N(Activities.ACTIVITY_CHANGE_INVOICE_ADDRESS_POSSIBLE),
                util._N(Gateways.GW_XOR_INVOICE_EXIST)
        );

        assertThat(orderProcess).hasPassed(
                util._N(Activities.ACTIVITY_SUB_PROCESS_INVOICE_ADDRESS_CHANGE),
                util._N(Events.EVENT_INVOICE_ADDRESS_NOT_CHANGED)
        );
        assertThat(orderProcess).hasNotPassed(util._N(Events.EVENT_END_MSG_INVOICE_ADDRESS_CHANGED));

        finishOrderProcess(orderProcess, orderId);

    }

    ProcessInstance createOrderProcess(SalesOrder salesOrder) {
        final String orderId = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderId, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(util._N(ItemVariables.SHIPMENT_METHOD), util._N(ShipmentMethod.PARCEL));
        processVariables.put(util._N(Variables.VAR_ORDER_ID), orderId);
        processVariables.put(util._N(Variables.VAR_PAYMENT_TYPE), "creditCard");
        processVariables.put(util._N(Variables.VAR_ORDER_VALID), true);
        processVariables.put(util._N(Variables.VAR_ORDER_ITEMS), orderItems);
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), "parcel");

        return runtimeService.createMessageCorrelation(util._N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                .processInstanceBusinessKey(orderId)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }

    void finishOrderProcess(final ProcessInstance orderProcess, final String orderId) {
        // start subprocess
        util.sendMessage(util._N(Messages.MSG_ORDER_RECEIVED_PAYMENT_SECURED), orderId);

        // send items thru
        util.sendMessage(util._N(ItemMessages.MSG_ITEM_TRANSMITTED), orderId);
        util.sendMessage(util._N(ItemMessages.MSG_PACKING_STARTED), orderId);
        util.sendMessage(util._N(ItemMessages.MSG_TRACKING_ID_RECEIVED), orderId);
        util.sendMessage(util._N(ItemMessages.MSG_ITEM_DELIVERED), orderId);

        assertThat(orderProcess).isEnded();
    }

    SalesOrder getSalesOrder() {
        return salesOrderService.createOrder(util.getRandomOrderNumber());
    }
}
