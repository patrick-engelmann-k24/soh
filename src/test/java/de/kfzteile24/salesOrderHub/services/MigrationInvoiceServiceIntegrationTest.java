package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static org.mockito.Mockito.verify;

@Slf4j
class MigrationInvoiceServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    private SnsPublishService snsPublishService;
    @Autowired
    private MigrationInvoiceService migrationInvoiceService;
    @Autowired
    private ObjectUtil objectUtil;

    private final MessageWrapper messageWrapper = MessageWrapper.builder()
            .receiveCount(1)
            .build();

    @SneakyThrows
    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreated() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";

        var message = getObjectByResource("coreSalesInvoiceCreatedMultipleItems.json", CoreSalesInvoiceCreatedMessage.class);

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderGroupId(originalOrderNumber);

        migrationInvoiceService.handleMigrationCoreSalesInvoiceCreated(message, messageWrapper);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
    }

    @SneakyThrows
    @Test
    void testQueueListenerMigrationCoreSalesInvoiceCreatedDuplicateSubsequentOrder() {


        var salesOrder = salesOrderUtil.createNewSalesOrder();

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku = "2010-10183";
        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);


        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderGroupId(originalOrderNumber);

        var migrationMessage = objectUtil.deepCopyOf(message, CoreSalesInvoiceCreatedMessage.class);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);

        migrationInvoiceService.handleMigrationCoreSalesInvoiceCreated(migrationMessage, messageWrapper);

        verify(snsPublishService).publishMigrationOrderRowCancelled(originalOrderNumber, rowSku);
        verify(snsPublishService).publishMigrationOrderCreated(newOrderNumberCreatedInSoh);
    }
}

