package de.kfzteile24.salesOrderHub.helper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.converter.CustomerTypeConverter;
import de.kfzteile24.salesOrderHub.converter.OrderHeaderConverter;
import de.kfzteile24.salesOrderHub.converter.OrderJsonConverter;
import de.kfzteile24.salesOrderHub.converter.OrderRowConverter;
import de.kfzteile24.salesOrderHub.converter.SurchargesConverter;
import de.kfzteile24.salesOrderHub.converter.TotalTaxesConverter;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
@RequiredArgsConstructor
public class SalesOrderUtil {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final BpmUtil bpmUtil;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final OrderJsonConverter orderJsonConverter;

    @SneakyThrows(JsonProcessingException.class)
    public SalesOrder createNewSalesOrder() {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        SqsMessage sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        OrderJSON orderJSON = objectMapper.readValue(sqsMessage.getBody(), OrderJSON.class);
        orderJSON.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());

        final SalesOrder testOrder = SalesOrder.builder()
                .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                .salesChannel(orderJSON.getOrderHeader().getOrigin().getSalesChannel())
                .originalOrder(orderJSON)
                .latestJson(orderJsonConverter.convert(orderJSON))
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
        final var orderJsonConverter = new OrderJsonConverter(
                new OrderHeaderConverter(
                        new CustomerTypeConverter(),
                        new TotalTaxesConverter(),
                        new SurchargesConverter()
                ),
                new OrderRowConverter());
        return SalesOrder.builder()
            .orderNumber("514000016")
            .salesChannel("www-k24-at")
            .customerEmail("test@kfzteile24.de")
            .recurringOrder(Boolean.TRUE)
            .originalOrder(orderJson)
            .latestJson(orderJsonConverter.convert(orderJson))
            .build();
    }

    @SneakyThrows(JsonProcessingException.class)
    public static OrderJSON getOrderJson(String rawMessage){
        ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
        final var sqsMessage = mapper.readValue(rawMessage, SqsMessage.class);
        return mapper.readValue(sqsMessage.getBody(), OrderJSON.class);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static OrderJSON readOrderJson(String path) {
        final var orderJson = readResource(path);
        return new ObjectMapperConfig().objectMapper().readValue(orderJson, OrderJSON.class);
    }
}
