package de.kfzteile24.salesOrderHub.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.aws.MessageHeader;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@Import(GsonConfig.class)
public class JsonOrderConfigTest {
    @NotNull
    @Autowired
    Gson gson;

    @NotNull
    @Autowired
    @Qualifier("messageHeader")
    Gson gsonMessage;

    @Test
    public void testCorrectInjection() {
        assertNotNull(gson);
        assertNotNull(gsonMessage);
        assertNotEquals(gson, gsonMessage);
    }

    @Test
    public void testEncodeAndDecodeJson() {
        var coreDataReader = new CoreDataReaderEvent();

        coreDataReader.setCreatedAt(LocalDateTime.now().toString());
        coreDataReader.setOrderNumber("1234567890");
        coreDataReader.setOrderItemSku("0987654321");

        final String json = gson.toJson(coreDataReader);

        final CoreDataReaderEvent event2 = gson.fromJson(json, CoreDataReaderEvent.class);

        assertEquals(coreDataReader.getCreatedAt(), event2.getCreatedAt());
    }

    @Test
    public void orderToObject() throws IOException {
        final String orderNUmber = "504000035";

        final MessageHeader messageHeader = gsonMessage.fromJson(loadJson(), MessageHeader.class);
        assertNotNull(messageHeader);
        assertNotNull(messageHeader.getMessageId());
        assertNotNull(messageHeader.getMessage());
        final OrderJSON orderJSON = gson.fromJson(messageHeader.getMessage(), OrderJSON.class);

        assertEquals(orderNUmber, orderJSON.getOrderHeader().getOrderNumber());

        final String json = gson.toJson(orderJSON, OrderJSON.class);

        assertTrue(json.length() > 100);
    }

    private String loadJson() throws IOException {
        String fileName = "examples/testmessage.json";
        ClassLoader classLoader = getClass().getClassLoader();

        File file = new File(Objects.requireNonNull(classLoader.getResource(fileName)).getFile());

        //Read File Content
        return new String(Files.readAllBytes(file.toPath()));
    }

    @Test
    public void JsonToLocalDateTime() {
        // ISO_LOCAL_DATE_TIME
        testDateString("2020-12-18T11:47:25.682190");
        testDateString("2020-12-18T11:47:25");

        // ISO_ZONED_DATE_TIME
        testDateString("2020-12-18T11:47:25+01:00");
        // RabbitMQ from fulfillment (ISO_ZONED_DATE_TIME)
        testDateString("2021-01-14T10:03:11.4588345+01:00");
        testDateString("2020-10-26T09:51:11.652Z");
    }

    private void testDateString(final String dateString ) {
        final String jsonString = "{\"created_at\":\"" + dateString + "\"}";
        final CoreDataReaderEvent event = testJsonDecodeForDateTime(jsonString);

        assertNotNull(event.getCreatedAt(), "testing format " + dateString);
    }

    private CoreDataReaderEvent testJsonDecodeForDateTime(final String json) {
        return gson.fromJson(json, CoreDataReaderEvent.class);
    }
}
