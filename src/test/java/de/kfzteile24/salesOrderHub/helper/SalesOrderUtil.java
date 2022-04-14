package de.kfzteile24.salesOrderHub.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.dto.events.SalesOrderCompletedEvent;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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

    @SneakyThrows(JsonProcessingException.class)
    public SalesOrder createNewSalesOrder() {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        SqsMessage sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        Order order = objectMapper.readValue(sqsMessage.getBody(), Order.class);
        order.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());

        final SalesOrder testOrder = SalesOrder.builder()
                .orderNumber(order.getOrderHeader().getOrderNumber())
                .orderGroupId(order.getOrderHeader().getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .originalOrder(order)
                .latestJson(order)
                .build();

        testOrder.setSalesOrderInvoiceList(new HashSet<>());
        salesOrderService.save(testOrder, ORDER_CREATED);
        return testOrder;
    }

    @SneakyThrows(JsonProcessingException.class)
    public SalesOrder createNewSalesOrderWithCustomSkusAndGroupId(String orderGroupId, String sku1, String sku2) {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        SqsMessage sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        Order order = objectMapper.readValue(sqsMessage.getBody(), Order.class);
        order.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());
        order.getOrderHeader().setOrderGroupId(orderGroupId);
        order.getOrderRows().get(0).setSku(sku1);
        order.getOrderRows().get(1).setSku(sku2);

        final SalesOrder testOrder = SalesOrder.builder()
                .orderNumber(order.getOrderHeader().getOrderNumber())
                .orderGroupId(order.getOrderHeader().getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .originalOrder(order)
                .latestJson(order)
                .build();

        testOrder.setSalesOrderInvoiceList(new HashSet<>());
        salesOrderService.save(testOrder, ORDER_CREATED);
        return testOrder;
    }

    @SneakyThrows(JsonProcessingException.class)
    public SalesOrder createNewSalesOrderHavingCancelledRow() {
        InputStream testFileStream = getClass().getResourceAsStream("/examples/testmessage.json");
        assertNotNull(testFileStream);

        SqsMessage sqsMessage = readTestFile(testFileStream);
        assertNotNull(sqsMessage);

        Order order = objectMapper.readValue(sqsMessage.getBody(), Order.class);
        order.getOrderHeader().setOrderNumber(bpmUtil.getRandomOrderNumber());
        order.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());

        Order latestJson = objectMapper.readValue(sqsMessage.getBody(), Order.class);
        latestJson.getOrderHeader().setOrderNumber(order.getOrderHeader().getOrderNumber());
        latestJson.getOrderHeader().setOrderGroupId(order.getOrderHeader().getOrderNumber());
        latestJson.setOrderRows(List.of(latestJson.getOrderRows().get(1)));

        final SalesOrder testOrder = SalesOrder.builder()
                .orderNumber(order.getOrderHeader().getOrderNumber())
                .orderGroupId(order.getOrderHeader().getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .originalOrder(order)
                .latestJson(latestJson)
                .build();

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

    public SalesOrder createPersistedSalesOrderV3WithDiffGroupId(
            boolean shouldContainVirtualItem,
            ShipmentMethod shipmentMethod,
            PaymentType paymentType,
            CustomerType customerType,
            String orderGroupId) {
        final var salesOrder = createNewSalesOrderV3(
                shouldContainVirtualItem, shipmentMethod, paymentType, customerType);
        salesOrder.setOrderGroupId(orderGroupId);

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
                .totals(Totals.builder()
                        .goodsTotalGross(BigDecimal.valueOf(100))
                        .goodsTotalNet(BigDecimal.valueOf(80))
                        .shippingCostGross(BigDecimal.valueOf(100))
                        .shippingCostNet(BigDecimal.valueOf(80))
                        .totalDiscountGross(BigDecimal.valueOf(90))
                        .totalDiscountNet(BigDecimal.valueOf(70))
                        .surcharges(Surcharges.builder()
                                .depositGross(BigDecimal.valueOf(100))
                                .depositNet(BigDecimal.valueOf(80))
                                .riskyGoodsGross(BigDecimal.valueOf(100))
                                .riskyGoodsNet(BigDecimal.valueOf(80))
                                .bulkyGoodsGross(BigDecimal.valueOf(100))
                                .bulkyGoodsNet(BigDecimal.valueOf(80))
                                .paymentGross(BigDecimal.valueOf(100))
                                .paymentNet(BigDecimal.valueOf(80))
                                .build())
                        .grandTotalTaxes(List.of(GrandTotalTaxes.builder()
                                .type("test")
                                .value(BigDecimal.ONE)
                                .rate(BigDecimal.ONE)
                                .build()))
                        .build())
                .payments(payments)
                .salesChannel("www-k24-at")
                .platform(Platform.ECP)
                .build();

        final Order order = Order.builder()
                .version("3.0")
                .orderHeader(orderHeader)
                .orderRows(orderRows)
                .build();

        return SalesOrder.builder()
                .orderNumber(orderNumber)
                .orderGroupId(orderNumber)
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .originalOrder(order)
                .latestJson(order)
                .recurringOrder(customerType == RECURRING)
                .build();
    }

    public static OrderRows createOrderRow(String sku, ShipmentMethod shippingType) {
        return OrderRows.builder()
                .sumValues(SumValues.builder()
                        .goodsValueGross(BigDecimal.valueOf(9))
                        .goodsValueNet(BigDecimal.valueOf(3))
                        .discountGross(BigDecimal.valueOf(3))
                        .discountNet(BigDecimal.valueOf(1))
                        .totalDiscountedGross(BigDecimal.valueOf(6))
                        .totalDiscountedNet(BigDecimal.valueOf(2))
                        .build())
                .shippingType(shippingType.getName())
                .isCancelled(false)
                .taxRate(BigDecimal.valueOf(21))
                .quantity(BigDecimal.ONE)
                .sku(sku)
                .build();
    }

    public static SalesOrder createSalesOrderFromOrder(Order order) {
        return SalesOrder.builder()
                .orderNumber(order.getOrderHeader()
                        .getOrderNumber())
                .orderGroupId(order.getOrderHeader()
                        .getOrderGroupId())
                .salesChannel(order.getOrderHeader()
                        .getSalesChannel())
                .customerEmail(order.getOrderHeader()
                        .getCustomer()
                        .getCustomerEmail())
                .originalOrder(order)
                .latestJson(order)
                .build();
    }

    public static SalesOrder createSalesOrderFromOrder(SalesOrder salesOrder) {
        return SalesOrder.builder()
                .orderNumber(salesOrder.getOrderNumber())
                .orderGroupId(salesOrder.getOrderGroupId())
                .salesChannel(salesOrder.getSalesChannel())
                .customerEmail(salesOrder.getCustomerEmail())
                .originalOrder(salesOrder.getOriginalOrder())
                .latestJson(salesOrder.getLatestJson())
                .build();
    }

    public static void updatePlatform(SalesOrder salesOrder, Platform platform) {
        salesOrder.getLatestJson().getOrderHeader().setPlatform(platform);
        salesOrder.setOriginalOrder(salesOrder.getLatestJson());
    }

    public static SalesOrder createNewSalesOrderV3WithPlatform(
            boolean shouldContainVirtualItem,
            ShipmentMethod shipmentMethod,
            PaymentType paymentType,
            Platform platformType,
            CustomerType customerType) {
        SalesOrder salesOrder = createNewSalesOrderV3(shouldContainVirtualItem, shipmentMethod, paymentType, customerType);
        updatePlatform(salesOrder, platformType);
        return salesOrder;
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

    public static SalesOrder getSalesOrder(String rawMessage) {
        final var order = getOrder(rawMessage);
        return SalesOrder.builder()
                .orderNumber("514000016")
                .orderGroupId("514000016")
                .salesChannel("www-k24-at")
                .customerEmail("test@kfzteile24.de")
                .recurringOrder(Boolean.TRUE)
                .originalOrder(order)
                .latestJson(order)
                .build();
    }

    @SneakyThrows(JsonProcessingException.class)
    public static Order getOrder(String rawMessage) {
        ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
        final var sqsMessage = mapper.readValue(rawMessage, SqsMessage.class);
        return mapper.readValue(sqsMessage.getBody(), Order.class);
    }

    @SneakyThrows(JsonProcessingException.class)
    public static SalesOrderCompletedEvent getOrderCompleted(String rawMessage) {
        ObjectMapper mapper = new ObjectMapperConfig().objectMapper();
        final var sqsMessage = mapper.readValue(rawMessage, SqsMessage.class);
        return mapper.readValue(sqsMessage.getBody(), SalesOrderCompletedEvent.class);
    }

    public static SalesOrderInvoice createSalesOrderInvoice(final String orderNumber, final boolean isCorrection) {
        final var sep = isCorrection ? "--" : "-";
        final var invoiceNumber = RandomStringUtils.randomNumeric(10);
        final var invoiceUrl = "s3://k24-invoices/www-k24-at/2020/08/12/" + orderNumber + sep + invoiceNumber + ".pdf";
        return SalesOrderInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .orderNumber(orderNumber)
                .url(invoiceUrl)
                .source(InvoiceSource.SOH)
                .build();
    }
}
