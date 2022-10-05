package de.kfzteile24.salesOrderHub.services.migration;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.services.MigrationCreditNoteService;
import de.kfzteile24.salesOrderHub.services.MigrationInvoiceService;
import de.kfzteile24.salesOrderHub.services.MigrationSalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationSqsReceiveService {

    private final FeatureFlagConfig featureFlagConfig;

    private final MessageErrorHandler messageErrorHandler;
    private final MigrationSalesOrderService migrationSalesOrderService;
    private final MigrationInvoiceService migrationInvoiceService;
    private final MigrationCreditNoteService migrationCreditNoteService;

    /**
     * Consume messages from sqs for migration core sales order created published by core-publisher
     */
    @SneakyThrows
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesOrderCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales order created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesOrderCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try{
            if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreMigrationCoreSalesOrder())) {
                log.info("Migration Core Sales Order is ignored");
            } else {
                migrationSalesOrderService.handleMigrationCoreSalesOrderCreated(rawMessage, senderId, receiveCount);
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for migration core sales invoice created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesInvoiceCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales invoice created message", dispatcher = true)
    public void queueListenerMigrationCoreSalesInvoiceCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            if (featureFlagConfig.getIgnoreMigrationCoreSalesInvoice()) {
                log.info("Migration Core Sales Invoice is ignored");
            } else {
                migrationInvoiceService.handleMigrationCoreSalesInvoiceCreated(rawMessage, senderId, receiveCount);
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for migration core sales credit note created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales credit note created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            if (featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote()) {
                log.info("Migration Core Sales Credit Note is ignored");
            } else {
                migrationCreditNoteService.handleMigrationCoreSalesCreditNoteCreated(rawMessage, senderId, receiveCount);
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }
}