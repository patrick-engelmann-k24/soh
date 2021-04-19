package de.kfzteile24.salesOrderHub.helper;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.gson.Gson;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderItem;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.order.Rows;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SalesOrderUtil {

    @Autowired
    SalesOrderService salesOrderService;

    @Autowired
    BpmUtil bpmUtil;

    @NotNull
    @Autowired
    Gson gson;

    @NotNull
    @Autowired
    @Qualifier("messageHeader")
    Gson gsonMessage;

    public SalesOrder createNewSalesOrder() {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        EcpOrder sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        OrderJSON orderJSON = gson.fromJson(sqsMessage.getMessage(), OrderJSON.class);
        orderJSON.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());

        final SalesOrder testOrder = de.kfzteile24.salesOrderHub.domain.SalesOrder.builder()
                .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                .salesLocale(orderJSON.getOrderHeader().getOrigin().getLocale())
                .originalOrder(orderJSON)
                .build();

        // Get Shipping Type
        List<LogisticalUnits> logisticalUnits = orderJSON.getLogisticalUnits();
        assertEquals(1, logisticalUnits.size());

        Set<SalesOrderItem> testOrderItems = new HashSet<>();

        List<Rows> orderItems = orderJSON.getOrderRows();
        orderItems.forEach(row-> {
            SalesOrderItem salesOrderItem = new SalesOrderItem();
            salesOrderItem.setQuantity( new BigDecimal(row.getQuantity().toString()));
            salesOrderItem.setStockKeepingUnit(row.getSku());
            salesOrderItem.setShippingType(logisticalUnits.get(0).getShippingType());
            testOrderItems.add(salesOrderItem);
        });

        testOrder.setSalesOrderItemList(testOrderItems);
        testOrder.setSalesOrderInvoiceList(new HashSet<>());
        salesOrderService.save(testOrder);
        return testOrder;
    }

    private EcpOrder readTestFile(InputStream testFileStream) {
        StringBuilder content = new StringBuilder();
        try (InputStreamReader streamReader =
                     new InputStreamReader(testFileStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }

            return gsonMessage.fromJson(content.toString(), EcpOrder.class);
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

    public static SalesOrder getSaleOrder(String rawMessage){
        return SalesOrder.builder()
            .orderNumber("514000018")
            .salesLocale(Locale.GERMANY.toString())
            .customerEmail("test@kfzteile24.de")
            .recurringOrder(Boolean.TRUE)
            .originalOrder(getOrderJson(rawMessage))
            .build();
    }

    @SneakyThrows(JsonProcessingException.class)
    public static OrderJSON getOrderJson(String rawMessage){
        ObjectMapper mapper = new ObjectMapper();
        String message = configureMapperForMessageHeader(mapper).readValue(rawMessage, EcpOrder.class).getMessage();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(message, OrderJSON.class);
    }

    private static ObjectMapper configureMapperForMessageHeader(ObjectMapper mapper){
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.UPPER_CAMEL_CASE);
        return mapper;
    }
}
