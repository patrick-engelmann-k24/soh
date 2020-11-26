package de.kfzteile24.salesOrderHub.configuration;

import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.aws.MessageHeader;
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
import java.util.Objects;

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
}
