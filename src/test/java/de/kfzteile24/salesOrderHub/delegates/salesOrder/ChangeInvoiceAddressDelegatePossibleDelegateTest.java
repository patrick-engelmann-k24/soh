package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.*;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import org.camunda.bpm.engine.ProcessEngine;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class ChangeInvoiceAddressDelegatePossibleDelegateTest {
    @Autowired
    public ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderInvoiceRepository invoiceRepository;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Before
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testChangeInvoiceAddressPossible() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(orderProcess).isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED, orderNumber);

        // check if the delegate sets the variable
        assertThat(orderProcess)
                .hasVariables(util._N(Variables.INVOICE_EXISTS));
        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), util._N(Variables.INVOICE_EXISTS));
        assertFalse("Variable invoice exists does not exist", invoiceExists);

        assertThat(orderProcess).hasPassedInOrder(
                util._N(Events.START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED),
                util._N(Activities.CHANGE_INVOICE_ADDRESS_POSSIBLE),
                util._N(Gateways.XOR_INVOICE_EXIST)
        );
        assertThat(orderProcess).hasPassed(
                util._N(Activities.CHANGE_INVOICE_ADDRESS),
                util._N(Activities.SUB_PROCESS_INVOICE_ADDRESS_CHANGE)
        );

        assertThat(orderProcess).hasNotPassed(util._N(Events.INVOICE_ADDRESS_NOT_CHANGED));

        finishOrderProcess(orderProcess, orderNumber);
    }

    @Test
    public void testChangeInvoiceAddressPossibleNotPossible() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();

        final SalesOrderInvoice orderInvoice = SalesOrderInvoice.builder()
                .salesOrder(testOrder)
                .invoiceNumber(util.getRandomOrderNumber())
                .build();
        invoiceRepository.save(orderInvoice);

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(orderProcess).isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED, orderNumber);

        // check if the delegate sets the variable
        assertThat(orderProcess)
                .hasVariables(util._N(Variables.INVOICE_EXISTS));
        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), util._N(Variables.INVOICE_EXISTS));
        assertTrue("Variable invoice exists does not exist", invoiceExists);

        assertThat(orderProcess).hasPassedInOrder(
                util._N(Events.START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED),
                util._N(Activities.CHANGE_INVOICE_ADDRESS_POSSIBLE),
                util._N(Gateways.XOR_INVOICE_EXIST)
        );

        assertThat(orderProcess).hasPassed(
                util._N(Activities.SUB_PROCESS_INVOICE_ADDRESS_CHANGE),
                util._N(Events.INVOICE_ADDRESS_NOT_CHANGED)
        );
        assertThat(orderProcess).hasNotPassed(util._N(Events.END_MSG_INVOICE_ADDRESS_CHANGED));

        finishOrderProcess(orderProcess, orderNumber);

    }

    private ProcessInstance createOrderProcess(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(util._N(Variables.SHIPMENT_METHOD), util._N(ShipmentMethod.REGULAR));
        processVariables.put(util._N(Variables.ORDER_NUMBER), orderNumber);
        processVariables.put(util._N(Variables.PAYMENT_TYPE), util._N(CREDIT_CARD));
        processVariables.put(util._N(Variables.ORDER_VALID), true);
        processVariables.put(util._N(Variables.ORDER_ROWS), orderItems);

        return runtimeService.createMessageCorrelation(util._N(Messages.ORDER_RECEIVED_MARKETPLACE))
                .processInstanceBusinessKey(orderNumber)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }

    void finishOrderProcess(final ProcessInstance orderProcess, final String orderNumber) {
        // start subprocess
        util.sendMessage(util._N(Messages.ORDER_RECEIVED_PAYMENT_SECURED), orderNumber);

        // send items thru
        util.sendMessage(util._N(RowMessages.ROW_TRANSMITTED_TO_LOGISTICS), orderNumber);
        util.sendMessage(util._N(RowMessages.PACKING_STARTED), orderNumber);
        util.sendMessage(util._N(RowMessages.TRACKING_ID_RECEIVED), orderNumber);
        util.sendMessage(util._N(RowMessages.ROW_SHIPPED), orderNumber);

        assertThat(orderProcess).isEnded();
    }
}
