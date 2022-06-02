package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderReturnRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
class SaveCreditNoteDelegateIntegrationTest {
    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private SalesOrderReturnRepository salesOrderReturnRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TimedPollingService pollingService;

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private BpmUtil bpmUtil;

    @BeforeEach
    public void setUp() {
        init(processEngine);
    }

    @Test
    void testInvoiceIsStoredCorrectly() {

        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrderRepository.save(salesOrder);
        final var creditNoteNumber = "20222" + RandomStringUtils.randomNumeric(5);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, "1234567");
        salesOrderReturnRepository.save(salesOrderReturn);
        Assertions.assertThat(salesOrderReturn.getUrl()).isNull();
        var invoiceUrl = String.format("s3://k24-invoices/app_android-kfzteile24-de/2021/06/04/%s.pdf", salesOrderReturn.getOrderNumber() + "-" + creditNoteNumber);


        camundaHelper.createReturnOrderProcess(salesOrderReturn, CORE_CREDIT_NOTE_CREATED);
        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(INVOICE_URL.getName(), invoiceUrl);
        processVariables.put(ORDER_NUMBER.getName(), salesOrderReturn.getOrderNumber());
        ProcessInstance processInstance =
                runtimeService.createMessageCorrelation(DROPSHIPMENT_CREDIT_NOTE_CREATED.getName())
                .processInstanceVariableEquals(ORDER_NUMBER.getName(), salesOrderReturn.getOrderNumber())
                .setVariables(processVariables)
                .correlateWithResult()
                .getProcessInstance();
        assertTrue(pollingService.poll(Duration.ofMillis(100), Duration.ofSeconds(10), () -> {
            assertThat(processInstance).hasPassed("eventStartMsgCreditNoteCreated",
                            "activitySaveCreditNoteUrl",
                            "eventCreditNoteSaved");
            return true;
        }));
        SalesOrderReturn returnOrder = salesOrderReturnRepository.findByOrderNumber(salesOrderReturn.getOrderNumber());
        Assertions.assertThat(returnOrder).isNotNull();
        Assertions.assertThat(returnOrder.getUrl()).isNotNull();
        Assertions.assertThat(returnOrder.getUrl()).isEqualTo(invoiceUrl);
    }

    @AfterEach
    public void cleanup() {
        bpmUtil.cleanUp();
        salesOrderRepository.deleteAll();
        salesOrderReturnRepository.deleteAll();
    }
}
