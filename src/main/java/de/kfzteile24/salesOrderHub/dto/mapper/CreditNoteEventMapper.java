package de.kfzteile24.salesOrderHub.dto.mapper;

import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CreditNoteEventMapper {

    SalesCreditNoteReceivedEvent toSalesCreditNoteReceivedEvent(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage);

    @Mapping(target = "salesCreditNote.salesCreditNoteHeader.orderNumber", source = "newOrderNumber")
    SalesCreditNoteCreatedMessage updateByOrderNumber(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage, String newOrderNumber);
}
