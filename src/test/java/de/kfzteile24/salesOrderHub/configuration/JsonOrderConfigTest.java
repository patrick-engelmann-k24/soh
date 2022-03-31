package de.kfzteile24.salesOrderHub.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.soh.order.dto.Order;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(ObjectMapperConfig.class)
class JsonOrderConfigTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @SneakyThrows(JsonProcessingException.class)
    void testEncodeAndDecodeJson() {
        var coreDataReader = new CoreDataReaderEvent();

        coreDataReader.setCreatedAt(LocalDateTime.now().toString());
        coreDataReader.setOrderNumber("1234567890");
        coreDataReader.setOrderItemSku("0987654321");

        final String json = objectMapper.writeValueAsString(coreDataReader);

        final CoreDataReaderEvent event2 = objectMapper.readValue(json, CoreDataReaderEvent.class);

        assertEquals(coreDataReader.getCreatedAt(), event2.getCreatedAt());
    }

    @Test
    void orderToObject() throws IOException {
        final String orderNUmber = "524001248";

        final var sqsMessage = objectMapper.readValue(loadJson(), SqsMessage.class);
        assertNotNull(sqsMessage);
        assertNotNull(sqsMessage.getMessageId());
        assertNotNull(sqsMessage.getBody());
        final Order order = objectMapper.readValue(sqsMessage.getBody(), Order.class);

        assertEquals(orderNUmber, order.getOrderHeader().getOrderNumber());

        final String json = objectMapper.writeValueAsString(order);
        final Order deserializedOrderJson = objectMapper.readValue(json, Order.class);
        assertEquals(order, deserializedOrderJson);
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String loadJson() {
        String fileName = "examples/testmessage.json";
        return Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(fileName))
                        .toURI()));
    }

    @Test
    void jsonToLocalDateTime() {
        // ISO_LOCAL_DATE_TIME
        assertDateString("2020-12-18T11:47:25.682190");
        assertDateString("2020-12-18T11:47:25");

        // ISO_ZONED_DATE_TIME
        assertDateString("2020-12-18T11:47:25+01:00");
        // RabbitMQ from fulfillment (ISO_ZONED_DATE_TIME)
        assertDateString("2021-01-14T10:03:11.4588345+01:00");
        assertDateString("2020-10-26T09:51:11.652Z");
    }

    @Test
    @SneakyThrows(JsonProcessingException.class)
    void localDateTimeToJson() {
        final var time = OffsetDateTime.of(2020, 10, 26, 9, 51, 11, 0, ZoneOffset.UTC);
        var serializedTime = objectMapper.writeValueAsString(time);
        assertEquals("\"2020-10-26T09:51:11Z\"", serializedTime);
    }

    private void assertDateString(final String dateString ) {
        final String jsonString = "{\"created_at\":\"" + dateString + "\"}";
        final CoreDataReaderEvent event = testJsonDecodeForDateTime(jsonString);

        assertNotNull(event.getCreatedAt(), "testing format " + dateString);
    }

    @SneakyThrows(JsonProcessingException.class)
    private CoreDataReaderEvent testJsonDecodeForDateTime(final String json) {
        return objectMapper.readValue(json, CoreDataReaderEvent.class);
    }
}
