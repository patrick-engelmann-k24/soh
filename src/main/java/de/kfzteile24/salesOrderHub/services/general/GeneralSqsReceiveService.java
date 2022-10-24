package de.kfzteile24.salesOrderHub.services.general;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeneralSqsReceiveService extends AbstractSqsReceiveService {

    private final ParcelShippedService parcelShippedService;
    private final CamundaHelper camundaHelper;

    /**
     * Consume messages from sqs for event invoice from core
     */
    @SqsListener(value = "${soh.sqs.queue.invoicesFromCore}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling InvoiceReceived message", dispatcher = true)
    public void queueListenerInvoiceReceivedFromCore(String message, MessageWrapper messageWrapper) {

        if (InvoiceUrlExtractor.matchesCreditNoteNumberPattern(message)) {
            log.info("url: {} is for credit note", message);
            camundaHelper.handleCreditNoteFromDropshipmentOrderReturn(message, messageWrapper);
        } else {
            log.info("url: {} is for invoice", message);
            camundaHelper.handleInvoiceFromCore(message, messageWrapper);
        }
    }

    /**
     * Consume messages from sqs for event parcel shipped
     * to trigger emails on soh-communication-service for regular orders
     */
    @SqsListener(value = "${soh.sqs.queue.parcelShipped}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling ParcelShipped message", dispatcher = true)
    public void queueListenerParcelShipped(ParcelShippedMessage message, MessageWrapper messageWrapper) {
        parcelShippedService.handleParcelShipped(message, messageWrapper);
    }
}
