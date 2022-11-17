package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.CreditNoteLine;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shared.Address;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.Customer;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.Platform;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Surcharges;
import de.kfzteile24.soh.order.dto.Totals;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.ORDER_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.RETURN_ORDER_NUMBER_PREFIX;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.NONE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByJson;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static org.assertj.core.api.Assertions.assertThat;

@Component
@RequiredArgsConstructor
public class SalesOrderUtil {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final InvoiceService invoiceService;

    @NonNull
    private final BpmUtil bpmUtil;

    public SalesOrder createNewSalesOrder() {
        return salesOrderService.save(createSalesOrder(), ORDER_CREATED);
    }

    public SalesOrder createNewDropshipmentSalesOrder() {
        SalesOrder salesOrder = createSalesOrder();
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        return salesOrderService.save(salesOrder, ORDER_CREATED);
    }

    private SalesOrder createSalesOrder() {

        Order order = getObjectByResource("testmessage.json", Order.class);
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

        final Customer customer = Customer.builder()
                .customerNumber(RandomStringUtils.randomAlphabetic(10)).build();

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
                .customer(customer)
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
                .rowKey(new Random().nextInt())
                .isPriceHammer(false)
                .name("name-" + sku)
                .unitValues(UnitValues.builder()
                        .goodsValueGross(BigDecimal.valueOf(9))
                        .goodsValueNet(BigDecimal.valueOf(3))
                        .discountGross(BigDecimal.valueOf(3))
                        .discountNet(BigDecimal.valueOf(1))
                        .discountedGross(BigDecimal.valueOf(6))
                        .discountedNet(BigDecimal.valueOf(2))
                        .build())
                .sumValues(SumValues.builder()
                        .goodsValueGross(BigDecimal.valueOf(9))
                        .goodsValueNet(BigDecimal.valueOf(3))
                        .discountGross(BigDecimal.valueOf(3))
                        .discountNet(BigDecimal.valueOf(1))
                        .totalDiscountedGross(BigDecimal.valueOf(6))
                        .totalDiscountedNet(BigDecimal.valueOf(2))
                        .build())
                .shippingType(shippingType.getName())
                .shippingAddressKey(1)
                .isCancelled(false)
                .taxRate(BigDecimal.valueOf(21))
                .quantity(BigDecimal.ONE)
                .sku(sku)
                .build();
    }

    public static SalesOrder createSalesOrderFromOrder(Order order) {
        return SalesOrder.builder()
                .orderNumber(order.getOrderHeader().getOrderNumber())
                .orderGroupId(order.getOrderHeader().getOrderNumber())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
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

    public static SalesOrderReturn getSalesOrderReturn(SalesOrder salesOrder, String creditNoteNumber) {
        return SalesOrderReturn.builder()
                .salesOrder(salesOrder)
                .orderNumber(createOrderNumberInSOH(salesOrder.getOrderNumber(), creditNoteNumber))
                .orderGroupId(salesOrder.getOrderGroupId())
                .returnOrderJson(salesOrder.getLatestJson())
                .build();
    }

    public static void assertSalesCreditNoteCreatedMessage(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage, SalesOrder salesOrder) {
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderGroupId()).isEqualTo(salesOrder.getOrderGroupId());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getCity()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getCity());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getCountryCode()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getCountryCode());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getStreet()).isEqualTo(Address.getStreet(salesOrder.getLatestJson().getOrderHeader().getBillingAddress()));
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getSalutation()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getSalutation());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getFirstName()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getFirstName());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getLastName()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getLastName());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getBillingAddress().getZipCode()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getBillingAddress().getZipCode());
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCurrencyCode()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getOrderCurrency());

        List<CreditNoteLine> creditNoteLines = new ArrayList<>(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteLines());
        assertThat(creditNoteLines).hasSize(2);
        assertThat(creditNoteLines.get(0).getIsShippingCost()).isFalse();
        assertThat(creditNoteLines.get(0).getQuantity()).isEqualTo(BigDecimal.valueOf(-2));
        assertThat(creditNoteLines.get(0).getUnitNetAmount()).isEqualTo(BigDecimal.valueOf(10.84));
        assertThat(creditNoteLines.get(0).getLineNetAmount()).isEqualTo(BigDecimal.valueOf(-21.68));
        assertThat(creditNoteLines.get(0).getUnitGrossAmount()).isEqualTo(BigDecimal.valueOf(12.91));
        assertThat(creditNoteLines.get(0).getLineGrossAmount()).isEqualTo(BigDecimal.valueOf(-25.82));
        assertThat(creditNoteLines.get(0).getItemNumber()).isEqualTo("2270-13013");
        assertThat(creditNoteLines.get(0).getLineTaxAmount()).isEqualTo(BigDecimal.valueOf(-4.14));
        assertThat(creditNoteLines.get(0).getTaxRate()).isEqualTo(BigDecimal.valueOf(19.00).setScale(2, RoundingMode.HALF_UP));

        assertThat(creditNoteLines.get(1).getIsShippingCost()).isFalse();
        assertThat(creditNoteLines.get(1).getQuantity()).isEqualTo(BigDecimal.valueOf(-2));
        assertThat(creditNoteLines.get(1).getUnitNetAmount()).isEqualTo(BigDecimal.valueOf(10.84));
        assertThat(creditNoteLines.get(1).getLineNetAmount()).isEqualTo(BigDecimal.valueOf(-21.68));
        assertThat(creditNoteLines.get(1).getUnitGrossAmount()).isEqualTo(BigDecimal.valueOf(12.91));
        assertThat(creditNoteLines.get(1).getLineGrossAmount()).isEqualTo(BigDecimal.valueOf(-25.82));
        assertThat(creditNoteLines.get(1).getItemNumber()).isEqualTo("2270-13012");
        assertThat(creditNoteLines.get(1).getLineTaxAmount()).isEqualTo(BigDecimal.valueOf(-4.14));
        assertThat(creditNoteLines.get(1).getTaxRate()).isEqualTo(BigDecimal.valueOf(19.00).setScale(2, RoundingMode.HALF_UP));

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getNetAmount()).isEqualTo(BigDecimal.valueOf(-43.36));
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getGrossAmount()).isEqualTo(BigDecimal.valueOf(-51.64).setScale(2, RoundingMode.HALF_UP));

    }

    public SalesOrder createSalesOrderForMigrationInvoiceTest() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var orderRows = List.of(
                OrderRows.builder()
                        .sumValues(SumValues.builder()
                                .goodsValueGross(BigDecimal.valueOf(3.38))
                                .goodsValueNet(BigDecimal.valueOf(2))
                                .build())
                        .unitValues(UnitValues.builder()
                                .goodsValueGross(BigDecimal.valueOf(3.38))
                                .goodsValueNet(BigDecimal.valueOf(2))
                                .build())
                        .taxRate(BigDecimal.valueOf(19))
                        .quantity(BigDecimal.valueOf(2))
                        .sku("sku-1")
                        .build()
        );

        var totals = Totals.builder()
                .goodsTotalGross(BigDecimal.valueOf(2.38))
                .goodsTotalNet(BigDecimal.valueOf(2))
                .grandTotalGross(BigDecimal.valueOf(2.38))
                .grandTotalNet(BigDecimal.valueOf(2))
                .paymentTotal(BigDecimal.valueOf(2.38))
                .grandTotalTaxes(List.of(GrandTotalTaxes.builder()
                        .rate(BigDecimal.valueOf(19))
                        .value(BigDecimal.valueOf(0.38))
                        .build()))
                .build();

        salesOrder.getLatestJson().getOrderHeader().setTotals(totals);
        salesOrder.getLatestJson().setOrderRows(orderRows);
        var order = salesOrder.getLatestJson();
        var orderNumber = "580309129";
        salesOrder.setOrderNumber(orderNumber);
        salesOrder.setOrderGroupId(orderNumber);
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        salesOrder.setOriginalOrder(order);
        salesOrder.setLatestJson(order);

        salesOrderService.save(salesOrder, ORDER_CREATED);
        return salesOrder;
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

    public static SalesOrder getSalesOrder(String rawMessage) {
        final var order = getObjectByJson(rawMessage, Order.class);
        return getSalesOrder(order);
    }

    public static SalesOrder getSalesOrder(Order order) {
        return SalesOrder.builder()
                .orderNumber(order.getOrderHeader().getOrderNumber())
                .orderGroupId(order.getOrderHeader().getOrderGroupId())
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
                .recurringOrder(Boolean.TRUE)
                .originalOrder(order)
                .latestJson(order)
                .build();
    }

    public static String createOrderNumberInSOH(String orderNumber, String reference) {
        return orderNumber + ORDER_NUMBER_SEPARATOR + reference;
    }

    public static String createReturnOrderNumberInSOH(String reference) {
        return RETURN_ORDER_NUMBER_PREFIX + ORDER_NUMBER_SEPARATOR + reference;
    }

    public Set<SalesOrderInvoice> getSalesOrderInvoices(final String orderNumber) {
        return invoiceService.getInvoicesByOrderNumber(orderNumber);
    }

    public static SalesOrderInvoice createSalesOrderInvoice(final String orderNumber, final boolean isCorrection) {
        final var sep = isCorrection ? "--" : "-";
        final var minusForOrderNumber = isCorrection ? "-" : "";
        final var invoiceNumber = "2022-1" + RandomStringUtils.randomNumeric(12);
        final var invoiceUrl = "s3://k24-invoices/www-k24-at/2020/08/12/" + orderNumber + sep + invoiceNumber + ".pdf";
        return SalesOrderInvoice.builder()
                .invoiceNumber(minusForOrderNumber + invoiceNumber)
                .orderNumber(orderNumber)
                .url(invoiceUrl)
                .source(InvoiceSource.SOH)
                .build();
    }

    public CoreSalesInvoiceCreatedMessage createInvoiceCreatedMsg(String orderNumber) {

        var message = getObjectByResource("dropshipmentInvoiceData.json", CoreSalesInvoiceCreatedMessage.class);
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(orderNumber);
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderGroupId(orderNumber);
        return message;
    }

    public SalesOrder createPersistedSalesOrder(LocalDateTime createdAt, String... skuList) {
        SalesOrder salesOrder = createSalesOrder(createdAt, skuList);

        return salesOrderService.save(salesOrder, ORDER_CREATED);
    }

    public SalesOrder createPersistedSalesOrder(String orderGroupId, LocalDateTime createdAt, String... skuList) {
        SalesOrder salesOrder = createSalesOrder(orderGroupId, createdAt, skuList);

        return salesOrderService.save(salesOrder, ORDER_CREATED);
    }

    public static SalesOrder createSalesOrder(LocalDateTime createdAt, String... skuList) {
        return createSalesOrder(null, createdAt, skuList);
    }

    public static SalesOrder createSalesOrder(String orderGroupId, LocalDateTime createdAt, String... skuList) {
        final String orderNumber = UUID.randomUUID().toString();
        List<OrderRows> orderRows = new ArrayList<>();
        for (String sku : skuList) {
            orderRows.add(createOrderRow(sku, REGULAR));
        }

        final OrderHeader orderHeader = OrderHeader.builder()
                .orderNumber(orderNumber)
                .orderGroupId(orderGroupId != null ? orderGroupId : orderNumber)
                .salesChannel("www-k24-at")
                .platform(Platform.ECP)
                .build();

        final Order order = Order.builder()
                .version("3.0")
                .orderHeader(orderHeader)
                .orderRows(orderRows)
                .build();
        SalesOrder salesOrder = SalesOrder.builder()
                .orderNumber(orderNumber)
                .orderGroupId(orderGroupId != null ? orderGroupId : orderNumber)
                .salesChannel(order.getOrderHeader().getSalesChannel())
                .originalOrder(order)
                .latestJson(order)
                .recurringOrder(false)
                .build();
        salesOrder.setCreatedAt(createdAt);
        return salesOrder;
    }
}
