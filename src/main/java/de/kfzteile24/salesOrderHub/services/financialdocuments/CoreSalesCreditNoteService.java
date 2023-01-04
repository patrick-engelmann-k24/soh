package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSalesCreditNoteService {

    private final SalesOrderReturnService salesOrderReturnService;
    private final FeatureFlagConfig featureFlagConfig;

    @EnrichMessageForDlq
    public void handleCoreSalesCreditNoteCreated(SalesCreditNoteCreatedMessage message, MessageWrapper messageWrapper) {

        if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreCoreCreditNote())) {
            log.info("Core Credit Note is ignored");
        } else {
            var orderNumber = message.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
            log.info("Received core sales credit note created message with order number: {}", orderNumber);
            salesOrderReturnService.handleSalesOrderReturn(message, RETURN_ORDER_CREATED, CORE_CREDIT_NOTE_CREATED);
        }
    }
}
