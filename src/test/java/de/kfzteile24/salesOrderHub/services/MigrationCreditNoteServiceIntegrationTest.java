package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createReturnOrderNumberInSOH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@Slf4j
class MigrationCreditNoteServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private SnsPublishService snsPublishService;

    @Autowired
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;

    @Autowired
    private SalesOrderUtil salesOrderUtil;

    @Autowired
    private MigrationCreditNoteService migrationCreditNoteService;

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

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createReturnOrderNumberInSOH(creditNumber));
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

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(message, messageWrapper);

        migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(argThat(
                (SalesOrderReturn salesOrderReturn) -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo(createReturnOrderNumberInSOH(creditNumber));
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo(orderNumber);
                    return true;
                }
        ));
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