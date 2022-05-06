package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.dto.events.CoreSalesInvoiceCreatedReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author samet
 */

@Mapper
public interface EventMapper {
    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    CoreSalesInvoiceCreatedReceivedEvent toCoreSalesInvoiceCreatedReceivedEvent(CoreSalesInvoiceCreatedMessage msg);
}
