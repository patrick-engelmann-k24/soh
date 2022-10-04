package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.CoreSalesCreditNoteCreatedService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
@Slf4j
class MigrationCreditNoteServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @Autowired
    private SalesOrderService salesOrderService;

    @Autowired
    private SnsPublishService snsPublishService;

    @Autowired
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MessageErrorHandler messageErrorHandler;

    @Autowired
    private SQSNamesConfig sqsNamesConfig;

    @Autowired
    private CreditNoteEventMapper creditNoteEventMapper;

    @Autowired
    private SalesOrderReturnService salesOrderReturnService;

    @Autowired
    private CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private MigrationCreditNoteService migrationCreditNoteService;

    @Autowired
    private MessageWrapperUtil messageWrapperUtil;

    @Autowired
    private TimedPollingService timedPollingService;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InvoiceNumberCounterRepository invoiceNumberCounterRepository;

    @Autowired
    private InvoiceNumberCounterService invoiceNumberCounterService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private BpmUtil bpmUtil;

    @BeforeEach
    public void setup() {
        super.setUp();
        bpmUtil.cleanUp();
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        invoiceNumberCounterRepository.deleteAll();
        invoiceNumberCounterService.init();
    }
    @Test
    @DisplayName("IT migration core sales credit note created event handling")
    void testHandleMigrationCoreSalesCreditNoteCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = salesOrderUtil.createSalesOrderForMigrationInvoiceTest();
        var orderNumber = salesOrder.getOrderNumber();
        var creditNumber = "876130";

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");
        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createOrderNumberInSOH(orderNumber, creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
    }

    @Test
    @DisplayName("IT migration core sales credit note created event handling if related return order already exists")
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedDuplicateReturnOrder(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = salesOrderUtil.createSalesOrderForMigrationInvoiceTest();
        var orderNumber = salesOrder.getOrderNumber();
        var creditNumber = "876130";

        var coreReturnDeliveryNotePrinted = readResource("examples/coreSalesCreditNoteCreated.json");

        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(coreReturnDeliveryNotePrinted, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(argThat(
                (SalesOrderReturn salesOrderReturn) -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createOrderNumberInSOH(orderNumber, creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> bpmUtil.cleanUp());
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        invoiceNumberCounterService.init();
    }
}