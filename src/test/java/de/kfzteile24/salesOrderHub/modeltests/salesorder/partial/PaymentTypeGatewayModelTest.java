package de.kfzteile24.salesOrderHub.modeltests.salesorder.partial;

import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.POSITIVE_PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PaymentTypeGateway model test")
@Slf4j(topic = "PaymentTypeGateway model test")
class PaymentTypeGatewayModelTest extends AbstractWorkflowTest {

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("PaymentTypeGatewayTest"))
    @DisplayName("Start process before gwXORCheckPaymentType. positivePaymentType is true")
    void testIsSohOrderGatewayTestPositivePaymentTypeTrue(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(POSITIVE_PAYMENT_TYPE.getName(), true);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_PAYMENT_SECURED.getName()))
                .thenReturn(RECEIVED_MESSAGE_CATCH_EVENT_ACTION);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_PAYMENT_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario, never()).hasStarted(MSG_ORDER_PAYMENT_SECURED.getName());
        verify(processScenario).waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName());
    }

    @Test
    @Tags(@Tag("PaymentTypeGatewayTest"))
    @DisplayName("Start process before gwXORCheckPaymentType. positivePaymentType is false")
    void testIsSohOrderGatewayTestPositivePaymentTypeFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(POSITIVE_PAYMENT_TYPE.getName(), false);

        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_PAYMENT_SECURED.getName()))
                .thenReturn(RECEIVED_MESSAGE_CATCH_EVENT_ACTION);
        when(processScenario.waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName()))
                .thenReturn(WAIT_MESSAGE_CATCH_EVENT_ACTION);

        scenario = startBeforeActivity(SALES_ORDER_PROCESS, XOR_CHECK_PAYMENT_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario).hasCompleted(MSG_ORDER_PAYMENT_SECURED.getName());
        verify(processScenario).waitsAtMessageIntermediateCatchEvent(MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName());
    }
}
