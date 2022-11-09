package de.kfzteile24.salesOrderHub.services.migration;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.MigrationCreditNoteService;
import de.kfzteile24.salesOrderHub.services.MigrationInvoiceService;
import de.kfzteile24.salesOrderHub.services.MigrationSalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationSqsReceiveService extends AbstractSqsReceiveService {

    private final FeatureFlagConfig featureFlagConfig;
    private final MigrationSalesOrderService migrationSalesOrderService;
    private final MigrationInvoiceService migrationInvoiceService;
    private final MigrationCreditNoteService migrationCreditNoteService;

    /**
     * Consume messages from sqs for migration core sales order created published by core-publisher
     */
    @SneakyThrows
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesOrderCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales order created message", dispatcher = true)
    public void queueListenerMigrationCoreSalesOrderCreated(Order message, MessageWrapper messageWrapper) {

        if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreMigrationCoreSalesOrder())) {
            log.info("Migration Core Sales Order is ignored");
        } else {
            migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(message, messageWrapper);
        }
    }

    /**
     * Consume messages from sqs for migration core sales invoice created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesInvoiceCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales invoice created message", dispatcher = true)
    public void queueListenerMigrationCoreSalesInvoiceCreated(CoreSalesInvoiceCreatedMessage message, MessageWrapper messageWrapper) {

        if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreMigrationCoreSalesInvoice())) {
            log.info("Migration Core Sales Invoice is ignored");
        } else {
            migrationInvoiceService.handleMigrationCoreSalesInvoiceCreated(message, messageWrapper);
        }
    }

    /**
     * Consume messages from sqs for migration core sales credit note created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales credit note created message", dispatcher = true)
    public void queueListenerMigrationCoreSalesCreditNoteCreated(SalesCreditNoteCreatedMessage message, MessageWrapper messageWrapper) {

        if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote())) {
            log.info("Migration Core Sales Credit Note is ignored");
        } else {
            migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(message, messageWrapper);
        }
    }
}