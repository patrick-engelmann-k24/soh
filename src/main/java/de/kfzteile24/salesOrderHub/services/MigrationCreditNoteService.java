package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationCreditNoteService {

    @NonNull
    private final OrderUtil orderUtil;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final CreditNoteEventMapper creditNoteEventMapper;

    @NonNull
    private final SalesOrderReturnService salesOrderReturnService;

    @NonNull
    private final FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;

    @Transactional
    public void handleMigrationCoreSalesCreditNoteCreated(SalesCreditNoteCreatedMessage message, MessageWrapper messageWrapper) {

        var salesCreditNoteHeader = message.getSalesCreditNote().getSalesCreditNoteHeader();
        var orderNumber = salesCreditNoteHeader.getOrderNumber();
        var creditNoteNumber = salesCreditNoteHeader.getCreditNoteNumber();

        var returnOrder = salesOrderReturnService.getReturnOrder(orderNumber, creditNoteNumber);
        if (returnOrder.isPresent()) {
            snsPublishService.publishMigrationReturnOrderCreatedEvent(returnOrder.get());
            log.info("Return order with order number {} and credit note number: {} is duplicated for migration. " +
                            "Publishing event on migration topic",
                    orderNumber,
                    creditNoteNumber);

            message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderNumber(returnOrder.get().getOrderNumber());
            var salesCreditNoteReceivedEvent =
                    creditNoteEventMapper.toSalesCreditNoteReceivedEvent(message);
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
            financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(message, messageWrapper);
        }
    }
}