package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteDocumentGeneratedEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CreditNoteService {

    @NonNull
    private final SalesOrderReturnService salesOrderReturnService;

    public SalesCreditNoteDocumentGeneratedEvent buildSalesCreditNoteDocumentGeneratedEvent(String orderNumber, String creditNoteDocumentLink) {
        SalesOrderReturn salesOrderReturn = salesOrderReturnService.getByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(orderNumber));
        return SalesCreditNoteDocumentGeneratedEvent
                .builder()
                .returnOrder(salesOrderReturn.getReturnOrderJson())
                .creditNoteDocumentLink(creditNoteDocumentLink)
                .build();
    }

}
