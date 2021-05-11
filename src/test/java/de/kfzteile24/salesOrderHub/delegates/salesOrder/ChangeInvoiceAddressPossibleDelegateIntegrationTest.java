package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.dto.order.customer.Address;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_ADDRESS_CHANGED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
        classes = SalesOrderHubProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
public class ChangeInvoiceAddressPossibleDelegateIntegrationTest {
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil util;

    @Autowired
    private SalesOrderInvoiceRepository invoiceRepository;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogUtil auditLogUtil;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    public void testChangeInvoiceAddressPossible() throws JsonProcessingException {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();
        final var newAddress = Address.builder().city("Berlin").build();

        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat(orderProcess).isWaitingAt(util._N(Events.MSG_ORDER_PAYMENT_SECURED));
        util.sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED.getName(), orderNumber,
                Map.of(
                        Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName(), objectMapper.writeValueAsString(newAddress))
                );

        // check if the delegate sets the variable
        assertThat(orderProcess)
                .hasVariables(util._N(Variables.INVOICE_EXISTS));
        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), util._N(Variables.INVOICE_EXISTS));
        assertFalse(invoiceExists, "Variable invoice exists does not exist");

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

        final var updatedOrderOpt = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedOrderOpt.isPresent());
        assertEquals(updatedOrderOpt.get().getLatestJson().getOrderHeader().getBillingAddress(), newAddress);
        assertNotEquals(updatedOrderOpt.get().getOriginalOrder(), updatedOrderOpt.get().getLatestJson());

        util.finishOrderProcess(orderProcess, orderNumber);

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), INVOICE_ADDRESS_CHANGED);
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
        assertTrue(invoiceExists, "Variable invoice exists does not exist");

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

        util.finishOrderProcess(orderProcess, orderNumber);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogDoesNotExist(testOrder.getId(), INVOICE_ADDRESS_CHANGED);

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
}
