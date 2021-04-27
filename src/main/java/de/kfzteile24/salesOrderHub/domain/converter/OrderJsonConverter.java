package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
@RequiredArgsConstructor
public class OrderJsonConverter implements AttributeConverter<OrderJSON, String> {

    private final ObjectMapper objectMapper;

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public String convertToDatabaseColumn(OrderJSON attribute) {
        return objectMapper.writeValueAsString(attribute);
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public OrderJSON convertToEntityAttribute(String dbData) {
        return objectMapper.readValue(dbData, OrderJSON.class);
    }
}
