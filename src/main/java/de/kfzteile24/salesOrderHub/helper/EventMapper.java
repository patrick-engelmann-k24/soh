package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author samet
 */

@Mapper
public interface EventMapper {
    EventMapper INSTANCE = Mappers.getMapper(EventMapper.class);

    CoreSalesFinancialDocumentLine toCoreSalesFinancialDocumentLine(CreditNoteLine item);
}
