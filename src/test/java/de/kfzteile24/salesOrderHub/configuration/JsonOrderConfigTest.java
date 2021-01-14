package de.kfzteile24.salesOrderHub.configuration;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.aws.MessageHeader;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
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
        var coreDataReader = CoreDataReaderEvent.builder()
                .createdAt(LocalDateTime.now())
                .orderNumber("1234567890")
                .orderItemSku("0987654321")
                .build();

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

        assertNotNull("testing format " + dateString, event);
    }

    private CoreDataReaderEvent testJsonDecodeForDateTime(final String json) {
        return gson.fromJson(json, CoreDataReaderEvent.class);
    }
}
