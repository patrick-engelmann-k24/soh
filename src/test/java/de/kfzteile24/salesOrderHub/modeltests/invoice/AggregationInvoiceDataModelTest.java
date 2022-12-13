package de.kfzteile24.salesOrderHub.modeltests.invoice;

import de.kfzteile24.salesOrderHub.delegates.invoicing.AggregateInvoiceDataDelegate;
import de.kfzteile24.salesOrderHub.delegates.invoicing.listener.IsPartialInvoiceListener;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.modeltests.AbstractWorkflowTest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.INVOICING_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.AGGREGATE_INVOICE_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DETERMINE_DROPSHIPMENT_ORDER_INVOICE_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_SUBSEQUENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_NUMBER_LIST;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("AggregationInvoiceData model test")
@Slf4j(topic = "AggregationInvoiceData model test")
class AggregationInvoiceDataModelTest extends AbstractWorkflowTest {

    @Autowired
    AggregateInvoiceDataDelegate aggregateInvoiceDataDelegate;

    @Autowired
    IsPartialInvoiceListener isPartialInvoiceListener;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        processVariables = createProcessVariables(salesOrder);
        businessKey = salesOrder.getOrderNumber();
    }

    @Test
    @Tags(@Tag("InvoiceDataAggregationTriggeredAndSubprocessesStartedTest"))
    @DisplayName("After the timer is triggered, start aggregation of invoice data and subprocesses.")
    @SneakyThrows
    void testInvoiceDataAggregationTriggeredAndSubprocessesStarted(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(INVOICE_NUMBER_LIST.getName(), List.of("I-123", "I-124", "I-125"));
        processVariables.put(IS_PARTIAL_INVOICE.getName(), false);

        scenario = startBeforeActivity(INVOICING_PROCESS, AGGREGATE_INVOICE_DATA.getName(),
                businessKey, processVariables);

        verify(processScenario, times(1)).hasCompleted(AGGREGATE_INVOICE_DATA.getName());
        verify(processScenario, times(3)).hasCompleted("eventStartSubInvoicing");
        verify(aggregateInvoiceDataDelegate).execute(any());

        assertThat(scenario.instance(processScenario)).isEnded();

    }

    @Test
    @Tags(@Tag("ActivityCreateDropshipmentSalesOrderInvoiceStartedForPartialInvoiceFalseTest"))
    @DisplayName("If isPartialInvoice false, start activityCreateDropshipmentSalesOrderInvoice.")
    @SneakyThrows
    void testActivityCreateDropshipmentSalesOrderInvoiceStartedForPartialInvoiceFalse(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(INVOICE_NUMBER.getName(), "I-101");
        processVariables.put(IS_PARTIAL_INVOICE.getName(), false);

        scenario = startBeforeActivity(INVOICING_PROCESS, DETERMINE_DROPSHIPMENT_ORDER_INVOICE_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario, times(1)).hasCompleted(DETERMINE_DROPSHIPMENT_ORDER_INVOICE_TYPE.getName());
        verify(processScenario, times(1)).hasCompleted(XOR_CHECK_PARTIAL_INVOICE.getName());
        verify(processScenario, times(1)).hasCompleted(INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE.getName());
        verify(processScenario, never()).hasCompleted(INVOICING_CREATE_SUBSEQUENT_ORDER.getName());
        verify(isPartialInvoiceListener).notify(any());

        assertThat(scenario.instance(processScenario)).isEnded();

    }

    @Test
    @Tags(@Tag("ActivityCreateInvoiceSubsequentOrderStartedForPartialInvoiceTrueTest"))
    @DisplayName("If isPartialInvoice true, start activityCreateInvoiceSubsequentOrder.")
    void testActivityCreateInvoiceSubsequentOrderStartedForPartialInvoiceTrue(TestInfo testinfo) {
        log.info("{} - {}", testinfo.getDisplayName(), testinfo.getTags());

        processVariables.put(INVOICE_NUMBER.getName(), "I-102");
        processVariables.put(IS_PARTIAL_INVOICE.getName(), true);

        scenario = startBeforeActivity(INVOICING_PROCESS, DETERMINE_DROPSHIPMENT_ORDER_INVOICE_TYPE.getName(),
                businessKey, processVariables);

        verify(processScenario, times(1)).hasCompleted(DETERMINE_DROPSHIPMENT_ORDER_INVOICE_TYPE.getName());
        verify(processScenario, times(1)).hasCompleted(XOR_CHECK_PARTIAL_INVOICE.getName());
        verify(processScenario, never()).hasCompleted(INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE.getName());
        verify(processScenario, times(1)).hasCompleted(INVOICING_CREATE_SUBSEQUENT_ORDER.getName());

        assertThat(scenario.instance(processScenario)).isEnded();

    }

}