package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.kfzteile24.salesOrderHub.SalesOrderHubProcessApplication;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
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

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CASH_ON_DELIVERY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.reset;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = SalesOrderHubProcessApplication.class)
class CheckPaymentTypeDelegateIntTest {

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
    public void isWaitingAtPaymentSecured() throws JsonProcessingException {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder, CREDIT_CARD);
        try {
            Thread.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertThat(orderProcess).isWaitingAt(Events.MSG_ORDER_PAYMENT_SECURED.getName());
    }

    @Test
    public void isNotWaitingForPaymentSecured() throws JsonProcessingException {
        final SalesOrder testOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = createOrderProcess(testOrder, CASH_ON_DELIVERY);
        assertThat(orderProcess).isNotWaitingFor(Events.MSG_ORDER_PAYMENT_SECURED.getName());
    }

    private ProcessInstance createOrderProcess(SalesOrder salesOrder, PaymentType paymentType) {
        final String orderNumber = salesOrder.getOrderNumber();
        final List<String> orderItems = util.getOrderRows(orderNumber, 5);

        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(Variables.SHIPMENT_METHOD.getName(), ShipmentMethod.REGULAR.getName());
        processVariables.put(Variables.ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(Variables.PAYMENT_TYPE.getName(), paymentType.getName());
        processVariables.put(Variables.ORDER_VALID.getName(), true);
        processVariables.put(Variables.ORDER_ROWS.getName(), orderItems);

        return runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_MARKETPLACE.getName())
                .processInstanceBusinessKey(orderNumber)
                .setVariables(processVariables)
                .correlateWithResult().getProcessInstance();
    }
}