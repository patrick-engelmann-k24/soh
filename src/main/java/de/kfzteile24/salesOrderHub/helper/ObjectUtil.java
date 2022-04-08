package de.kfzteile24.salesOrderHub.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
@Value
public class ObjectUtil {

    ObjectMapper mapper;

    @SneakyThrows
    public <T> T deepCopyOf(T object, Class<T> clazz) {
        String jsonString = mapper.writeValueAsString(object);
        return mapper.readValue(jsonString, clazz);
    }
}
