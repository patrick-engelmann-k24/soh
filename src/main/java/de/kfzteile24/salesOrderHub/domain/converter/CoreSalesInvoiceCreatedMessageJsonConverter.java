package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
@RequiredArgsConstructor
public class CoreSalesInvoiceCreatedMessageJsonConverter implements AttributeConverter<Object, String> {

    @NonNull
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public String convertToDatabaseColumn(Object jsonValue) {
        return objectMapper.writeValueAsString(jsonValue);
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public Object convertToEntityAttribute(String jsonValue) {
        return objectMapper.readValue(jsonValue, CoreSalesInvoiceCreatedMessage.class);
    }
}
