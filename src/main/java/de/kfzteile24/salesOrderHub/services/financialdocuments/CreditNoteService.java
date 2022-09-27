package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteCreatedEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CreditNoteService {


    @NonNull
    private final SalesOrderReturnService salesOrderReturnService;

    public SalesCreditNoteCreatedEvent buildSalesCreditNoteCreatedEvent(String orderNumber, String creditNoteDocumentLink) {
        SalesOrderReturn salesOrderReturn = Optional.of(salesOrderReturnService.getByOrderNumber(orderNumber))
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(orderNumber));
        return SalesCreditNoteCreatedEvent
                .builder()
                .returnOrder(salesOrderReturn.getReturnOrderJson())
                .creditNoteDocumentLink(creditNoteDocumentLink)
                .build();
    }



}
