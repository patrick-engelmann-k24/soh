package de.kfzteile24.salesOrderHub.domain.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.stream.Stream;

@Converter
public class SalesOrderInvoiceSourceConverter implements AttributeConverter<InvoiceSource, String> {
    @Override
    public String convertToDatabaseColumn(InvoiceSource source) {
        return source != null ? source.getName() : null;
    }

    @Override
    public InvoiceSource convertToEntityAttribute(String source) {
        return Stream.of(InvoiceSource.values())
                .filter(c -> c.getName().equals(source))
                .findAny()
                .orElse(null);
    }
}
