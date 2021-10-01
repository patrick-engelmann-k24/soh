package de.kfzteile24.salesOrderHub.helper;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.converter.CustomerTypeConverter;
import de.kfzteile24.salesOrderHub.converter.OrderHeaderConverter;
import de.kfzteile24.salesOrderHub.converter.OrderJsonConverter;
import de.kfzteile24.salesOrderHub.converter.OrderRowConverter;
import de.kfzteile24.salesOrderHub.converter.SurchargesConverter;
import de.kfzteile24.salesOrderHub.converter.TotalTaxesConverter;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.order.LogisticalUnits;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
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

    public SalesOrder createPersistedSalesOrderV3(
            boolean shouldContainVirtualItem,
            ShipmentMethod shipmentMethod,
            PaymentType paymentType,
            CustomerType customerType) {
        final var salesOrder = createNewSalesOrderV3(
                shouldContainVirtualItem, shipmentMethod, paymentType, customerType);

        salesOrderService.save(salesOrder, ORDER_CREATED);

        return salesOrder;
    }

    public static SalesOrder createNewSalesOrderV3(
            boolean shouldContainVirtualItem,
            ShipmentMethod shipmentMethod,
            PaymentType paymentType,
            CustomerType customerType) {
        final String orderNumber = UUID.randomUUID().toString();
        final List<OrderRows> orderRows = List.of(
                createOrderRow("sku-1", shouldContainVirtualItem ? NONE : shipmentMethod),
                createOrderRow("sku-2", shipmentMethod),
                createOrderRow("sku-3", shipmentMethod)
        );

        final List<Payments> payments = List.of(
                Payments.builder()
                        .type(paymentType.getName())
                        .build()
        );

        final OrderHeader orderHeader = OrderHeader.builder()
                .orderNumber(orderNumber)
                .payments(payments)
                .salesChannel("www-k24-at")
                .build();

        final Order order = Order.builder()
                .version("3.0")
                .orderHeader(orderHeader)
                .orderRows(orderRows)
                .build();

        return SalesOrder.builder()
                .orderNumber(orderNumber)
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .originalOrder(order)
                .latestJson(order)
                .recurringOrder(customerType == RECURRING)
                .build();
    }

    public static OrderRows createOrderRow(String sku, ShipmentMethod shippingType) {
       return OrderRows.builder()
               .shippingType(shippingType.getName())
               .sku(sku)
               .build();
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

    public static SalesOrderInvoice createSalesOrderInvoice(final String orderNumber, final boolean isCorrection) {
        final var sep = isCorrection ? "--" : "-";
        final var invoiceNumber = RandomStringUtils.randomNumeric(10);
        final var invoiceUrl = "s3://k24-invoices/www-k24-at/2020/08/12/" + orderNumber + sep + invoiceNumber + ".pdf";
        return SalesOrderInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .orderNumber(orderNumber)
                .url(invoiceUrl)
                .build();
    }
}
