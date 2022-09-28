package de.kfzteile24.salesOrderHub.services.financialdocuments;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
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
public class FinancialDocumentsSqsReceiveService {
    private final CoreSalesInvoiceCreatedService coreSalesInvoiceCreatedService;
    private final CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;
    private final SQSNamesConfig sqsNamesConfig;

    /**
     * Consume messages from sqs for core sales credit note created published by core-publisher
     */
    @SqsListener(value = "${soh.sqs.queue.coreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows
    @Trace(metricName = "Handling core sales credit note created message", dispatcher = true)
    public void queueListenerCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String sqsName = sqsNamesConfig.getCoreSalesCreditNoteCreated();
        coreSalesCreditNoteCreatedService.handleCoreSalesCreditNoteCreated(rawMessage, receiveCount, sqsName);
    }

    /**
     * Consume messages from sqs for core sales invoice created
     */
    @SneakyThrows
    @Transactional
    @SqsListener(value = "${soh.sqs.queue.coreSalesInvoiceCreated}")
    @Trace(metricName = "Handling core sales invoice created message", dispatcher = true)
    public void queueListenerCoreSalesInvoiceCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String sqsName = sqsNamesConfig.getCoreSalesInvoiceCreated();
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(rawMessage, receiveCount, sqsName);
    }
}
