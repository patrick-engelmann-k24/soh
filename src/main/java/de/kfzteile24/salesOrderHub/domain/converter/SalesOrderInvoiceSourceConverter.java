package de.kfzteile24.salesOrderHub.domain.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Objects;
import java.util.stream.Stream;

@Converter
public class SalesOrderInvoiceSourceConverter implements AttributeConverter<InvoiceSource, String> {
    @Override
    public String convertToDatabaseColumn(InvoiceSource source) {
        Objects.requireNonNull(source, "The invoice source must not be null");
        return source.getName();
    }

    @Override
    public InvoiceSource convertToEntityAttribute(String source) {
        return Stream.of(InvoiceSource.values())
                .filter(c -> c.getName().equals(source))
                .findAny()
                .orElseThrow(IllegalArgumentException::new);
    }
}
