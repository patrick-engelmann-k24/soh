package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
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
class CheckOrderTypeDelegateIntTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private BpmUtil util;

    @Autowired
    private RuntimeService runtimeService;

    @BeforeEach
    public void setUp() {
        reset();
        init(processEngine);
    }



    @Test
    void isWaitingAtPaymentSecured() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder, "support-kfzteile24-de");
        final var isWaitingForPaymentSecured =
                util.isProcessWaitingAtExpectedToken(orderProcess, Events.MSG_ORDER_PAYMENT_SECURED.getName());
        assertTrue(isWaitingForPaymentSecured);
    }

    @Test
    void isNotWaitingForPaymentSecured() {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder, "otc_branch3-kfzteile24-des");
        assertThat(orderProcess).isNotWaitingFor(Events.MSG_ORDER_PAYMENT_SECURED.getName());
    }

    private ProcessInstance createOrderProcess(SalesOrder salesOrder, String salesChannel) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(Variables.SHIPMENT_METHOD.getName(), ShipmentMethod.REGULAR.getName());
        processVariables.put(Variables.ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(Variables.PAYMENT_TYPE.getName(), CREDIT_CARD.getName());
        processVariables.put(Variables.ORDER_VALID.getName(), true);
        processVariables.put(Variables.ORDER_ROWS.getName(), orderItems);
        processVariables.put(Variables.SALES_CHANNEL.getName(), salesChannel);

        return runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                .processInstanceBusinessKey(orderNumber)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }
}