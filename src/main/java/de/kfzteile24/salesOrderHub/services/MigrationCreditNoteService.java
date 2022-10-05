package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationCreditNoteService {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final CreditNoteEventMapper creditNoteEventMapper;

    @NonNull
    private final SalesOrderReturnService salesOrderReturnService;

    @NonNull
    private final FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @NonNull
    private final MessageWrapperUtil messageWrapperUtil;

    @Transactional
    public void handleMigrationCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        var messageWrapper =
                messageWrapperUtil.create(rawMessage, SalesCreditNoteCreatedMessage.class);
        var salesCreditNoteCreatedMessage = messageWrapper.getMessage();
        var salesCreditNoteHeader = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader();
        var orderNumber = salesCreditNoteHeader.getOrderNumber();
        var creditNoteNumber = salesCreditNoteHeader.getCreditNoteNumber();

        var returnOrder = salesOrderReturnService.getByOrderNumber(
                salesOrderService.createOrderNumberInSOH(orderNumber, creditNoteNumber));
        if (returnOrder != null) {
            snsPublishService.publishMigrationReturnOrderCreatedEvent(returnOrder);
            log.info("Return order with order number {} and credit note number: {} is duplicated for migration. " +
                            "Publishing event on migration topic",
                    orderNumber,
                    creditNoteNumber);

            var salesCreditNoteReceivedEvent =
                    creditNoteEventMapper.toSalesCreditNoteReceivedEvent(salesCreditNoteCreatedMessage);
            snsPublishService.publishCreditNoteReceivedEvent(salesCreditNoteReceivedEvent);
            log.info("Publishing migration credit note created event for order number {} and credit note number: " +
                            "{}",
                    orderNumber,
                    creditNoteNumber);
        } else {
            log.info("Return order with order number {} and credit note number: {} is a new order." +
                            "Call redirected to normal flow.",
                    orderNumber,
                    creditNoteNumber);
            financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(rawMessage, senderId, receiveCount);
        }
    }
}