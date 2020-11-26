package de.kfzteile24.salesOrderHub.domain.converter;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class OrderJsonConverter implements AttributeConverter<OrderJSON, String> {

    @Autowired
    Gson gson;

    @Override
    public String convertToDatabaseColumn(OrderJSON attribute) {
        return gson.toJson(attribute);
    }

    @Override
    public OrderJSON convertToEntityAttribute(String dbData) {
        return gson.fromJson(dbData, OrderJSON.class);
    }
}
