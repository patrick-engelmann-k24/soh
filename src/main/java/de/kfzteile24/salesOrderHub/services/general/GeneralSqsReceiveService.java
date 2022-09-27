package de.kfzteile24.salesOrderHub.services.general;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeneralSqsReceiveService {

    private final MessageWrapperUtil messageWrapperUtil;
    private final ParcelShippedService parcelShippedService;
    private final SQSNamesConfig sqsNamesConfig;
    private final CamundaHelper camundaHelper;

    /**
     * Consume messages from sqs for event invoice from core
     */
    @SqsListener(value = "${soh.sqs.queue.invoicesFromCore}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling InvoiceReceived message", dispatcher = true)
    public void queueListenerInvoiceReceivedFromCore(String rawMessage,
                                                     @Header("SenderId") String senderId,
                                                     @Header("ApproximateReceiveCount") Integer receiveCount) {
        var messageWrapper = messageWrapperUtil.create(rawMessage, String.class);
        final var invoiceUrl = messageWrapper.getMessage();

        try {
            if (InvoiceUrlExtractor.matchesCreditNoteNumberPattern(invoiceUrl)) {
                camundaHelper.handleCreditNoteFromDropshipmentOrderReturn(invoiceUrl);
            } else {
                camundaHelper.handleInvoiceFromCore(invoiceUrl);
            }
        } catch (Exception e) {
            log.error("Invoice received from core message error - invoice url: {}\r\nErrorMessage: {}", invoiceUrl, e);
            throw e;
        }
    }

    /**
     * Consume messages from sqs for event parcel shipped
     * to trigger emails on soh-communication-service for regular orders
     */
    @SqsListener(value = "${soh.sqs.queue.parcelShipped}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling ParcelShipped message", dispatcher = true)
    public void queueListenerParcelShipped(String rawMessage,
                                           @Header("SenderId") String senderId,
                                           @Header("ApproximateReceiveCount") Integer receiveCount) {
        String sqsName = sqsNamesConfig.getParcelShipped();
        parcelShippedService.handleParcelShipped(rawMessage, receiveCount, sqsName);
    }
}
