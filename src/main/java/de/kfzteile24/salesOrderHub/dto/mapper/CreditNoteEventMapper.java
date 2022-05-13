package de.kfzteile24.salesOrderHub.dto.mapper;

import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CreditNoteEventMapper {

    SalesCreditNoteReceivedEvent toSalesCreditNoteReceivedEvent(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage);
}
