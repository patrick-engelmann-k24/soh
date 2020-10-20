package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.*;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.PaymentType.PAYMENT_CREDIT_CARD;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        final String orderNumber = testOrder.getOrderNumber();

        assertThat(orderProcess).isWaitingAt(util._N(Events.EVENT_MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.MSG_ORDER_INVOICE_ADDESS_CHANGE_RECEIVED, orderNumber);

        // check if the delegate sets the variable
        assertThat(orderProcess)
                .hasVariables(util._N(Variables.VAR_INVOICE_EXISTS));
        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), util._N(Variables.VAR_INVOICE_EXISTS));
        assertFalse("Variable invoice exists does not exist", invoiceExists);

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

        finishOrderProcess(orderProcess, orderNumber);
    }

    @Test
    public void testChangeInvoiceAddressPossibleNotPossible() {
        final SalesOrder testOrder = getSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();

        final SalesOrderInvoice orderInvoice = SalesOrderInvoice.builder()
                .salesOrder(testOrder)
                .invoiceNumber(util.getRandomOrderNumber())
                .build();
        invoiceRepository.save(orderInvoice);

        assertThat(orderProcess).isWaitingAt(util._N(Events.EVENT_MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.MSG_ORDER_INVOICE_ADDESS_CHANGE_RECEIVED, orderNumber);

        // check if the delegate sets the variable
        assertThat(orderProcess)
                .hasVariables(util._N(Variables.VAR_INVOICE_EXISTS));
        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), util._N(Variables.VAR_INVOICE_EXISTS));
        assertTrue("Variable invoice exists does not exist", invoiceExists);

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

        finishOrderProcess(orderProcess, orderNumber);

    }

    ProcessInstance createOrderProcess(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderItems(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(util._N(Variables.VAR_SHIPMENT_METHOD), util._N(ShipmentMethod.SHIPMENT_REGULAR));
        processVariables.put(util._N(Variables.VAR_ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.VAR_PAYMENT_TYPE), util._N(PAYMENT_CREDIT_CARD));
        processVariables.put(util._N(Variables.VAR_ORDER_VALID), true);
        processVariables.put(util._N(Variables.VAR_ORDER_ITEMS), orderItems);

        return runtimeService.createMessageCorrelation(util._N(Messages.MSG_ORDER_RECEIVED_MARKETPLACE))
                .processInstanceBusinessKey(orderNumber)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }

    void finishOrderProcess(final ProcessInstance orderProcess, final String orderNumber) {
        // start subprocess
        util.sendMessage(util._N(Messages.MSG_ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        // send items thru
        util.sendMessage(util._N(ItemMessages.MSG_ITEM_TRANSMITTED_TO_LOGISTICS), orderNumber);
        util.sendMessage(util._N(ItemMessages.MSG_PACKING_STARTED), orderNumber);
        util.sendMessage(util._N(ItemMessages.MSG_TRACKING_ID_RECEIVED), orderNumber);
        util.sendMessage(util._N(ItemMessages.MSG_ITEM_DELIVERED), orderNumber);

        assertThat(orderProcess).isEnded();
    }

    SalesOrder getSalesOrder() {
        return salesOrderService.createOrder(util.getRandomOrderNumber());
    }
}
