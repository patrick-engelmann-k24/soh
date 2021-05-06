package de.kfzteile24.salesOrderHub.helper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Component
public class SalesOrderUtil {

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    BpmUtil bpmUtil;

    @Autowired
    ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public SalesOrder createNewSalesOrder() {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        SqsMessage sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        OrderJSON orderJSON = objectMapper.readValue(sqsMessage.getBody(), OrderJSON.class);
        orderJSON.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());

        final SalesOrder testOrder = de.kfzteile24.salesOrderHub.domain.SalesOrder.builder()
                .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                .salesChannel(orderJSON.getOrderHeader().getOrigin().getSalesChannel())
                .originalOrder(orderJSON)
                .latestJson(orderJSON)
                .build();

        // Get Shipping Type
        List<LogisticalUnits> logisticalUnits = orderJSON.getLogisticalUnits();
        assertEquals(1, logisticalUnits.size());

        testOrder.setSalesOrderInvoiceList(new HashSet<>());
        salesOrderService.save(testOrder, ORDER_CREATED);
        return testOrder;
    }

    private SqsMessage readTestFile(InputStream testFileStream) {
        StringBuilder content = new StringBuilder();
        try (InputStreamReader streamReader =
                     new InputStreamReader(testFileStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            return objectMapper.readValue(content.toString(), SqsMessage.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    public static String readResource(String path) {
        return Files.readString(Paths.get(
            Objects.requireNonNull(SalesOrderUtil.class.getClassLoader().getResource(path))
                .toURI()));
    }

    public static SalesOrder getSalesOrder(String rawMessage){
        final var orderJson = getOrderJson(rawMessage);
        return SalesOrder.builder()
            .orderNumber("514000016")
            .salesChannel("www-k24-at")
            .customerEmail("test@kfzteile24.de")
            .recurringOrder(Boolean.TRUE)
            .originalOrder(orderJson)
            .latestJson(orderJson)
            .build();
    }

    public static SalesOrderInfo getSalesOrderInfo(String rawMessage) {
        return SalesOrderInfo.builder()
                .recurringOrder(Boolean.TRUE)
                .order(SalesOrderUtil.getOrderJson(rawMessage))
                .build();
    }
  
    @SneakyThrows(JsonProcessingException.class)
    public static OrderJSON getOrderJson(String rawMessage){
        ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
        final var sqsMessage = mapper.readValue(rawMessage, SqsMessage.class);
        return mapper.readValue(sqsMessage.getBody(), OrderJSON.class);
    }
}
