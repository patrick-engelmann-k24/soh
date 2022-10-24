package de.kfzteile24.salesOrderHub.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.util.ResourceUtils.CLASSPATH_URL_PREFIX;

public class JsonTestUtil {

    private static final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    public static final String EXAMPLES_SUB_FOLDER = "examples";

    @SneakyThrows
    public static <T> T getObjectByResource(String jsonResource, Class<T> clazz) {
        return objectMapper.readValue(getFile(jsonResource), clazz);
    }

    @SneakyThrows
    public static <T> T getObjectByJson(String json, Class<T> clazz) {
        return objectMapper.readValue(json, clazz);
    }

    public static Order copyOrderJson(Order orderJson) {
        return deepCopyOf(orderJson, Order.class);
    }

    @SneakyThrows
    public static <T> T deepCopyOf(T object, Class<T> clazz) {
        String jsonString = objectMapper.writeValueAsString(object);
        return objectMapper.readValue(jsonString, clazz);
    }

    public static File getFile(String jsonResource) throws FileNotFoundException {
        return ResourceUtils.getFile(CLASSPATH_URL_PREFIX + Paths.get(EXAMPLES_SUB_FOLDER, jsonResource));
    }

    @SneakyThrows
    public static String readJsonResource(String jsonResource) {
        return Files.readString(Path.of(getFile(jsonResource).toURI()));
    }
}
