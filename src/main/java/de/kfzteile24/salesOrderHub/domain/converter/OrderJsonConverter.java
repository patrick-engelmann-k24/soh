package de.kfzteile24.salesOrderHub.domain.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
@RequiredArgsConstructor
public class OrderJsonConverter implements AttributeConverter<Object, String> {

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final OrderJsonVersionDetector orderJsonVersionDetector;

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public String convertToDatabaseColumn(Object orderJson) {
        return objectMapper.writeValueAsString(orderJson);
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public Object convertToEntityAttribute(String orderJson) {
        if (orderJsonVersionDetector.isVersion3(orderJson)) {
            return objectMapper.readValue(orderJson, Order.class);
        } else {
            throw new IllegalStateException("Cannot convert unsupported JSON version: " +
                    orderJsonVersionDetector.detectVersion(orderJson));
        }
    }
}
