package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
@RequiredArgsConstructor
public class CreditNoteMessageConverter implements AttributeConverter<Object, String> {

    @NonNull
    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public String convertToDatabaseColumn(Object attribute) {
        return objectMapper.writeValueAsString(attribute);
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public Object convertToEntityAttribute(String dbData) {
        return dbData == null ? null : objectMapper.readValue(dbData, SalesCreditNoteCreatedMessage.class);
    }
}
