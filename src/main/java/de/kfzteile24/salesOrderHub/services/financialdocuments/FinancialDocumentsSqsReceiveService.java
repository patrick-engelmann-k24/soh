package de.kfzteile24.salesOrderHub.services.financialdocuments;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.dto.sns.PaypalRefundInstructionSuccessfulEvent;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class FinancialDocumentsSqsReceiveService extends AbstractSqsReceiveService {

    private final CoreSalesInvoiceService coreSalesInvoiceCreatedService;
    private final CoreSalesCreditNoteService coreSalesCreditNoteCreatedService;
    private final PaypalRefundInstructionSuccessfulService paypalRefundInstructionSuccessfulService;

    /**
     * Consume messages from sqs for core sales credit note created published by core-publisher
     */
    @SqsListener(value = "${soh.sqs.queue.coreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling core sales credit note created message", dispatcher = true)
    public void queueListenerCoreSalesCreditNoteCreated(SalesCreditNoteCreatedMessage message,
                                                        MessageWrapper messageWrapper) {

        coreSalesCreditNoteCreatedService.handleCoreSalesCreditNoteCreated(message, messageWrapper);
    }

    /**
     * Consume messages from sqs for core sales invoice created
     */

    @Transactional
    @SqsListener(value = "${soh.sqs.queue.coreSalesInvoiceCreated}")
    @Trace(metricName = "Handling core sales invoice created message", dispatcher = true)
    public void queueListenerCoreSalesInvoiceCreated(CoreSalesInvoiceCreatedMessage message,
                                                     MessageWrapper messageWrapper) {

        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);
    }

    /**
     * Consume messages from sqs for PayPal refund instruction successful
     */
    @Transactional
    @SqsListener(value = "${soh.sqs.queue.paypalRefundInstructionSuccessful}")
    @Trace(metricName = "Handling paypal refund instruction successful message", dispatcher = true)
    public void queueListenerPaypalRefundInstructionSuccessful(PaypalRefundInstructionSuccessfulEvent message,
                                                               MessageWrapper messageWrapper) {

        paypalRefundInstructionSuccessfulService.handlePaypalRefundInstructionSuccessful(message, messageWrapper);
    }
}
