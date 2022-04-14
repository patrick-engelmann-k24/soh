package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_ADDRESS_CHANGED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.helper.AuditLogUtil;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.BillingAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class ChangeInvoiceAddressPossibleDelegateIntegrationTest {

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

    @Autowired
    private TimedPollingService pollingService;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    void testChangeInvoiceAddressPossible() throws JsonProcessingException {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();
        final var newAddress = BillingAddress.builder().city("Berlin").build();

        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED, orderNumber,
                Map.of(
                        Variables.INVOICE_ADDRESS_CHANGE_REQUEST.getName(), objectMapper.writeValueAsString(newAddress))
                );

        // check if the delegate sets the variable
        final var invoiceExistsVariableHasBeenAdded = pollingService.pollWithDefaultTiming(() -> {
            assertThat(orderProcess)
                    .hasVariables(Variables.INVOICE_EXISTS.getName());
            return true;
        });
        assertTrue(invoiceExistsVariableHasBeenAdded);

        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), Variables.INVOICE_EXISTS.getName());
        assertFalse(invoiceExists, "Variable invoice exists does not exist");

        assertThat(orderProcess).hasPassedInOrder(
                Events.START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED.getName(),
                Activities.CHANGE_INVOICE_ADDRESS_POSSIBLE.getName(),
                Gateways.XOR_INVOICE_EXIST.getName()
        );
        assertThat(orderProcess).hasPassed(
                Activities.CHANGE_INVOICE_ADDRESS.getName(),
                Activities.SUB_PROCESS_INVOICE_ADDRESS_CHANGE.getName()
        );

        assertThat(orderProcess).hasNotPassed(Events.INVOICE_ADDRESS_NOT_CHANGED.getName());

        final var updatedOrderOpt = salesOrderService.getOrderByOrderNumber(orderNumber);
        assertTrue(updatedOrderOpt.isPresent());
        assertEquals(updatedOrderOpt.get().getLatestJson().getOrderHeader().getBillingAddress(), newAddress);
        assertNotEquals(updatedOrderOpt.get().getOriginalOrder(), updatedOrderOpt.get().getLatestJson());

        util.finishOrderProcess(orderProcess, orderNumber);

        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), INVOICE_ADDRESS_CHANGED);
    }

    @Test
    void testChangeInvoiceAddressNotPossible() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder);
        final String orderNumber = testOrder.getOrderNumber();

        final SalesOrderInvoice orderInvoice = SalesOrderInvoice.builder()
                .salesOrder(testOrder)
                .invoiceNumber(util.getRandomOrderNumber())
                .orderNumber(orderNumber)
                .source(InvoiceSource.SOH)
                .build();
        invoiceRepository.save(orderInvoice);

        assertTrue(util.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        util.sendMessage(Messages.ORDER_INVOICE_ADDRESS_CHANGE_RECEIVED, orderNumber);

        // check if the delegate sets the variable
        assertThat(orderProcess)
                .hasVariables(Variables.INVOICE_EXISTS.getName());
        final Boolean invoiceExists = (Boolean) runtimeService
                .getVariable(orderProcess.getProcessInstanceId(), Variables.INVOICE_EXISTS.getName());
        assertTrue(invoiceExists, "Variable invoice exists does not exist");

        assertThat(orderProcess).hasPassedInOrder(
                Events.START_MSG_INVOICE_ADDRESS_CHANGE_RECEIVED.getName(),
                Activities.CHANGE_INVOICE_ADDRESS_POSSIBLE.getName(),
                Gateways.XOR_INVOICE_EXIST.getName()
        );

        assertThat(orderProcess).hasPassed(
                Activities.SUB_PROCESS_INVOICE_ADDRESS_CHANGE.getName(),
                Events.INVOICE_ADDRESS_NOT_CHANGED.getName()
        );
        assertThat(orderProcess).hasNotPassed(Events.END_MSG_INVOICE_ADDRESS_CHANGED.getName());

        util.finishOrderProcess(orderProcess, orderNumber);
        auditLogUtil.assertAuditLogExists(testOrder.getId(), ORDER_CREATED);
        auditLogUtil.assertAuditLogDoesNotExist(testOrder.getId(), INVOICE_ADDRESS_CHANGED);

    }

    private ProcessInstance createOrderProcess(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(Variables.SHIPMENT_METHOD.getName(), ShipmentMethod.REGULAR.getName());
        processVariables.put(Variables.ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(Variables.PAYMENT_TYPE.getName(), CREDIT_CARD.getName());
        processVariables.put(Variables.ORDER_VALID.getName(), true);
        processVariables.put(Variables.ORDER_ROWS.getName(), orderItems);

        return runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                .processInstanceBusinessKey(orderNumber)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }
}
