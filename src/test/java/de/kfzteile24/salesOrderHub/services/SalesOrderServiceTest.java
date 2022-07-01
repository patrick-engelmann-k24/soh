package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.PaymentProviderData;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.Platform;
import org.camunda.bpm.engine.RuntimeService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.UnusedPrivateMethod"})
class SalesOrderServiceTest {

    @Mock
    private SalesOrderRepository salesOrderRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private RuntimeService runtimeService;

    @InjectMocks
    private SalesOrderService salesOrderService;

    private static final String paypalType = "paypal";
    private static final String creditcardType = "creditcard";
    private static final UUID uuid = UUID.randomUUID();
    private static final String externalId = "866560d1878642839eec6a414d1e8e1a";
    private static final BigDecimal transactionAmount = BigDecimal.valueOf(175.09);
    private static final String transactionId = "dd07626e5c76412ba494ec9d824a6bb1";
    private static final String description = "Authentication completed correctly";
    private static final String status = "OK";
    private static final String providerCode = "00000000";
    private static final String cardNumber = "0505210887207822";
    private static final String expiryDate = "2025-06-01";
    private static final String cardBrand = "Mastercard";

    @ParameterizedTest
    @MethodSource("provideParamsForRecurringOrderTest")
    void recurringOrdersAreIdentifiedCorrectly(long orderCount, boolean expectedResult) {
        final var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessage.json"));

        when(salesOrderRepository.countByCustomerEmail(salesOrder.getCustomerEmail())).thenReturn(orderCount);

        final boolean isRecurring = salesOrderService.isRecurringOrder(salesOrder);
        assertThat(isRecurring).isEqualTo(expectedResult);
    }

    @Test
    void createSalesOrder() {
        String rawMessage =  readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);
        final var existingInvoices = Set.of(
                SalesOrderInvoice.builder()
                        .orderNumber(salesOrder.getOrderNumber())
                        .invoiceNumber("1")
                        .source(InvoiceSource.SOH)
                        .build(),
                SalesOrderInvoice.builder()
                        .orderNumber(salesOrder.getOrderNumber())
                        .invoiceNumber("2")
                        .source(InvoiceSource.SOH)
                        .build()
        );
        when(salesOrderRepository.countByCustomerEmail(any())).thenReturn(1L);// 1 > 0
        when(invoiceService.getInvoicesByOrderNumber(salesOrder.getOrderNumber())).thenReturn(existingInvoices);
        when(salesOrderRepository.save(any())).thenReturn(salesOrder);

        SalesOrder createdSalesOrder = salesOrderService.createSalesOrder(salesOrder);

        assertThat(createdSalesOrder.isRecurringOrder()).isTrue();
        assertThat(createdSalesOrder.getLatestJson()).isNotNull();
        assertThat(createdSalesOrder.getCustomerEmail()).isEqualTo(salesOrder.getCustomerEmail());
        assertThat(createdSalesOrder.getSalesChannel()).isEqualTo(salesOrder.getSalesChannel());
        assertThat(createdSalesOrder.getOrderNumber()).isEqualTo(salesOrder.getOrderNumber());
        assertThat(createdSalesOrder.getOrderGroupId()).isEqualTo(salesOrder.getOrderGroupId());//The orderNumber should be used to fill the group Id with the same number, since it was missing in the Order JSON.

        verify(invoiceService).getInvoicesByOrderNumber(eq(salesOrder.getOrderNumber()));
        existingInvoices
                .forEach(invoice -> verify(invoiceService).addSalesOrderToInvoice(eq(salesOrder), eq(invoice)));

        verify(auditLogRepository).save(any());

    }

    @Test
    void savingAnOrderAlsoInsertsAnAuditLogEntry() {
        var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessage.json"));
        salesOrder.setId(UUID.randomUUID());
        when(salesOrderRepository.save(eq(salesOrder))).thenReturn(salesOrder);

        salesOrderService.save(salesOrder, ORDER_CREATED);

        verify(salesOrderRepository).save(salesOrder);
        verify(auditLogRepository).save(argThat(auditLog -> {
            assertThat(auditLog.getSalesOrderId()).isEqualTo(salesOrder.getId());
            assertThat(auditLog.getAction()).isEqualTo(ORDER_CREATED);
            assertThat(auditLog.getData()).isEqualTo(salesOrder.getLatestJson());
            return true;
        }));
    }

    private static Stream<Arguments> provideParamsForRecurringOrderTest() {
        return Stream.of(
                Arguments.of(1L, true),
                Arguments.of(0L, false)
        );
    }

    @Test
    void testCreateSubsequentSalesOrder() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        var originalOrderGroupId = salesOrder.getOrderGroupId();
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        String newOrderNumber = "22222";
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createCoreSalesInvoiceCreatedMessage(
                salesOrder.getOrderNumber(),
                orderRow.getSku());

        // Mock services
        when(invoiceService.getInvoicesByOrderNumber(any())).thenReturn(Set.of());
        when(orderUtil.createNewOrderRow(any(), any(), any())).thenReturn(orderRow);
        when(salesOrderRepository.save(any())).thenAnswer((Answer<SalesOrder>) invocation -> invocation.getArgument(0));

        // Establish some updates before the test in order to see the change
        GrandTotalTaxes actualGrandTotalTax = GrandTotalTaxes.builder()
                .rate(BigDecimal.TEN)
                .value(BigDecimal.TEN)
                .build();
        salesOrder.getLatestJson().getOrderHeader().getTotals().setGrandTotalTaxes(List.of(actualGrandTotalTax));

        var createdSalesOrder = salesOrderService.createSalesOrderForInvoice(
                invoiceCreatedMessage,
                salesOrder,
                newOrderNumber);

        assertThat(createdSalesOrder.getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(createdSalesOrder.getOrderGroupId()).isEqualTo(originalOrderGroupId);
        assertThat(createdSalesOrder.getOriginalOrder()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isEqualTo(Platform.SOH);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
        assertThat(createdSalesOrder.getId()).isNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderRows()).hasSize(1);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGoodsTotalGross()).isEqualTo(orderRow.getSumValues().getGoodsValueGross());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGoodsTotalNet()).isEqualTo(orderRow.getSumValues().getGoodsValueNet());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getTotalDiscountGross()).isEqualTo(orderRow.getSumValues().getDiscountGross());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGrandTotalGross()).isEqualTo(orderRow.getSumValues().getGoodsValueGross().subtract(orderRow.getSumValues().getDiscountGross()));
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGrandTotalTaxes()).isEqualTo(
                List.of(GrandTotalTaxes.builder()
                        .rate(orderRow.getTaxRate())
                        .value(new BigDecimal("28.48"))
                        .build())
        );
        invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(newOrderNumber);
        assertThat(createdSalesOrder.getInvoiceEvent()).isEqualTo(invoiceCreatedMessage);
    }


    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for No Shipping Cost LInes")
    void testFullyMatchedWithOriginalOrderNoShippingCostLines() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder, null, null, null);

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for Unknown Sku Shipping Cost LIne")
    void testFullyMatchedWithOriginalOrderUnknownSkuShippingCostLine() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder, "test", null, null);

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }


    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for Existing Sku Shipping Cost LIne")
    void testFullyMatchedWithOriginalOrderExistingSkuShippingCostLine() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder,
                orderRow.getSku(), null, null);

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is NOT Fully Matched With Original Order for NOT NULL And NULL ShippingCostNet")
    void testNotFullyMatchedWithOriginalOrderExistingSkuShippingCostLine() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);
        salesOrder.getLatestJson().getOrderHeader().getTotals().setShippingCostNet(BigDecimal.valueOf(0.1));

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder,
                orderRow.getSku(), null, null);

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertFalse(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for NOT NULL Matching ShippingCostNet")
    void testFullyMatchedWithOriginalOrderNotNullShippingCostNet() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);
        salesOrder.getLatestJson().getOrderHeader().getTotals().setShippingCostNet(BigDecimal.valueOf(0.1));

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder,
                orderRow.getSku(), BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.3));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertFalse(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }


    private CoreSalesInvoiceCreatedMessage createCoreSalesInvoiceCreatedMessage(String orderNumber, String sku) {
        BigDecimal quantity = BigDecimal.valueOf(1L);
        CoreSalesFinancialDocumentLine item = CoreSalesFinancialDocumentLine.builder()
                .itemNumber(sku)
                .quantity(quantity)
                .unitNetAmount(BigDecimal.valueOf(9))
                .lineNetAmount(BigDecimal.valueOf(9).multiply(quantity))
                .unitGrossAmount(BigDecimal.valueOf(9.8))
                .unitGrossAmount(BigDecimal.valueOf(9.8).multiply(quantity))
                .taxRate(BigDecimal.TEN)
                .isShippingCost(false)
                .build();

        CoreSalesInvoiceHeader coreSalesInvoiceHeader = new CoreSalesInvoiceHeader();
        coreSalesInvoiceHeader.setOrderNumber(orderNumber);
        coreSalesInvoiceHeader.setInvoiceNumber("50");
        coreSalesInvoiceHeader.setInvoiceLines(List.of(item));

        CoreSalesInvoice coreSalesInvoice = new CoreSalesInvoice();
        coreSalesInvoice.setSalesInvoiceHeader(coreSalesInvoiceHeader);

        CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage = new CoreSalesInvoiceCreatedMessage();
        salesInvoiceCreatedMessage.setSalesInvoice(coreSalesInvoice);
        return salesInvoiceCreatedMessage;
    }

    private CoreSalesInvoiceCreatedMessage createFullyMatchedItemsMessage(
            SalesOrder order, String shippingCostLineSku,
            BigDecimal shippingCostUnitNetAmount, BigDecimal shippingCostUnitGrossAmount) {
        CoreSalesInvoiceHeader coreSalesInvoiceHeader = new CoreSalesInvoiceHeader();
        coreSalesInvoiceHeader.setOrderNumber(order.getOrderNumber());
        CoreSalesInvoice coreSalesInvoice = new CoreSalesInvoice();
        coreSalesInvoice.setSalesInvoiceHeader(coreSalesInvoiceHeader);
        var invoiceLines = new ArrayList<CoreSalesFinancialDocumentLine>();
        for (OrderRows orderRow: order.getLatestJson().getOrderRows()) {
            CoreSalesFinancialDocumentLine item = CoreSalesFinancialDocumentLine.builder()
                    .itemNumber(orderRow.getSku())
                    .quantity(orderRow.getQuantity())
                    .taxRate(orderRow.getTaxRate())
                    .unitNetAmount(orderRow.getUnitValues().getGoodsValueNet())
                    .lineNetAmount(orderRow.getSumValues().getGoodsValueNet())
                    .unitGrossAmount(orderRow.getUnitValues().getGoodsValueGross())
                    .lineGrossAmount(orderRow.getSumValues().getGoodsValueGross())
                    .isShippingCost(false)
                    .build();
            invoiceLines.add(item);
        }

        if (shippingCostLineSku != null) {
            CoreSalesFinancialDocumentLine item = CoreSalesFinancialDocumentLine.builder()
                    .itemNumber(shippingCostLineSku)
                    .unitNetAmount(shippingCostUnitNetAmount)
                    .unitGrossAmount(shippingCostUnitGrossAmount)
                    .quantity(null)
                    .isShippingCost(true)
                    .build();
            invoiceLines.add(item);
        }

        coreSalesInvoiceHeader.setInvoiceLines(invoiceLines);

        CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage = new CoreSalesInvoiceCreatedMessage();
        salesInvoiceCreatedMessage.setSalesInvoice(coreSalesInvoice);
        return salesInvoiceCreatedMessage;
    }

    private void updateRowIsCancelledFieldAsTrue(SalesOrder salesOrder) {
        var order = salesOrder.getLatestJson();
        order.getOrderRows().forEach(r -> r.setIsCancelled(true));
        salesOrder.setLatestJson(order);
        salesOrder.setOriginalOrder(order);
    }

    @Test
    void testGetOrderNumberListByOrderGroupIdForMultipleSalesOrdersHavingRightSku(){
        var salesOrderList = new ArrayList<SalesOrder>();
        String rawMessage =  readResource("examples/ecpOrderMessage.json");

        // create multiple sales orders to be accepted and returned as result
        var salesOrder = getSalesOrder(rawMessage);
        salesOrder.setOrderNumber("1111");
        salesOrder.setOrderGroupId("123");
        salesOrderList.add(salesOrder); // 1 order
        var salesOrder2 = getSalesOrder(rawMessage);
        salesOrder2.setOrderNumber("2222");
        salesOrder2.setOrderGroupId("123");
        salesOrderList.add(salesOrder2); // 2 orders

        // create sales order to be rejected due to is cancelled field in row
        var salesOrderToBeRejected = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrderToBeRejected); //In order to observe change
        salesOrderToBeRejected.setOrderNumber("3333");
        salesOrderToBeRejected.setOrderGroupId("123");
        salesOrderList.add(salesOrderToBeRejected); // 3 orders

        // create sales order to be rejected due to different sku number
        var salesOrderToBeRejected2 = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrderToBeRejected2); //In order to observe change
        salesOrderToBeRejected2.setOrderNumber("4444");
        salesOrderToBeRejected2.setOrderGroupId("123");
        salesOrderToBeRejected2.getLatestJson().getOrderRows().get(0).setSku("98765432");
        salesOrderList.add(salesOrderToBeRejected2); // 4 orders

        when(salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(any())).thenReturn(salesOrderList);

        var orderNumberList = salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder2.getLatestJson().getOrderRows().get(0).getSku());

        assertThat(orderNumberList).contains("1111");
        assertThat(orderNumberList).contains("2222");
        assertThat(orderNumberList).doesNotContain("3333");
        assertThat(orderNumberList).doesNotContain("4444");
    }

    @Test
    void testGetOrderNumberListByOrderGroupIdForNoneSalesOrderHavingRightSku(){
        var salesOrderList = new ArrayList<SalesOrder>();
        String rawMessage =  readResource("examples/ecpOrderMessage.json");

        // create sales order to be rejected due to different sku number
        var salesOrder = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        salesOrder.setOrderGroupId("123");
        salesOrder.getLatestJson().getOrderRows().get(0).setSku("98765432");
        salesOrderList.add(salesOrder); // 4 orders

        when(salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(any())).thenReturn(salesOrderList);

        assertThatThrownBy(() -> salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder.getLatestJson().getOrderRows().get(0).getSku()))
                .isInstanceOf(SalesOrderNotFoundCustomException.class)
                .hasMessageContaining("Sales order not found for the given order group id",
                        salesOrder.getOrderGroupId());
    }

    @Test
    void testGetOrderNumberListByOrderGroupIdForNoneSalesOrderInGroupId(){
        var salesOrderList = new ArrayList<SalesOrder>();
        String rawMessage =  readResource("examples/ecpOrderMessage.json");

        // create sales order to be rejected due to different sku number
        var salesOrder = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        salesOrder.getLatestJson().getOrderRows().get(0).setSku("98765432");
        salesOrderList.add(salesOrder); // 4 orders

        when(salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(any())).thenReturn(salesOrderList);

        assertThatThrownBy(() -> salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder.getLatestJson().getOrderRows().get(0).getSku()))
                .isInstanceOf(SalesOrderNotFoundCustomException.class)
                .hasMessageContaining("Sales order not found for the given order group id",
                        salesOrder.getOrderGroupId(),
                        "order row sku number",
                        salesOrder.getLatestJson().getOrderRows().get(0).getSku());
    }

    @Test
    void testUpdateSalesOrderByOrderJson(){

        // Prepare sales order
        var orderNumber = "872634242";
        var originalSalesOrder = getSalesOrder(readResource("examples/testmessage.json"));
        var originalProviderData = getPaypalPaymentProviderData(originalSalesOrder);
        updateOriginalSalesOrder(orderNumber, originalSalesOrder);

        // Prepare new order json
        var orderJson = getOrderJson(orderNumber, false);

        when(salesOrderRepository.save(any())).thenAnswer((Answer<SalesOrder>) invocation -> invocation.getArgument(0));

        salesOrderService.updateSalesOrderByOrderJson(originalSalesOrder, orderJson);
        var orderHeader = ((Order) originalSalesOrder.getOriginalOrder()).getOrderHeader();
        var paypalPayment = orderHeader.getPayments().stream().filter(p -> p.getType().equals(paypalType)).findFirst().get();
        var paypalProviderData = paypalPayment.getPaymentProviderData();
        var creditcardPayment = orderHeader.getPayments().stream().filter(p -> p.getType().equals(creditcardType)).findFirst().get();
        var creditcardProviderData = creditcardPayment.getPaymentProviderData();
        assertThat(orderHeader.getOrderNumber()).contains(orderNumber);
        assertThat(orderHeader.getOrderGroupId()).contains(orderNumber);
        assertThat(originalSalesOrder.getOrderGroupId()).isEqualTo(originalSalesOrder.getOrderNumber());
        assertThat(originalSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId()).isEqualTo(originalSalesOrder.getOrderNumber());
        assertThat(((Order)originalSalesOrder.getOriginalOrder()).getOrderHeader().getOrderGroupId()).isEqualTo(originalSalesOrder.getOrderNumber());
        assertThat(paypalPayment.getPaymentTransactionId()).isNotEqualTo(UUID.fromString("4b3826ac-48a2-42d3-a724-2ecfa0737f47"));
        assertThat(paypalProviderData.getExternalId()).isEqualTo(originalProviderData.getExternalId());
        assertThat(paypalProviderData.getTransactionAmount()).isEqualTo(originalProviderData.getTransactionAmount());
        assertThat(paypalProviderData.getSenderFirstName()).isEqualTo(originalProviderData.getSenderFirstName());
        assertThat(paypalProviderData.getSenderLastName()).isEqualTo(originalProviderData.getSenderLastName());
        assertThat(paypalProviderData.getExternalTransactionId()).isEqualTo(originalProviderData.getExternalTransactionId());
        assertThat(paypalProviderData.getExternalPaymentStatus()).isEqualTo(originalProviderData.getExternalPaymentStatus());
        assertThat(paypalProviderData.getBillingAgreement()).isEqualTo(originalProviderData.getBillingAgreement());
        assertThat(creditcardPayment.getPaymentTransactionId()).isEqualTo(uuid);
        assertThat(creditcardProviderData.getExternalId()).isEqualTo(externalId);
        assertThat(creditcardProviderData.getTransactionAmount()).isEqualTo(BigDecimal.valueOf(180.99));
        assertThat(creditcardProviderData.getPaymentProviderTransactionId()).isEqualTo(transactionId);
        assertThat(creditcardProviderData.getPaymentProviderPaymentId()).isEqualTo(externalId);
        assertThat(creditcardProviderData.getPaymentProviderStatus()).isEqualTo("DONE");
        assertThat(creditcardProviderData.getPaymentProviderOrderDescription()).isEqualTo(description);
        assertThat(creditcardProviderData.getPaymentProviderCode()).isEqualTo(providerCode);
        assertThat(creditcardProviderData.getPseudoCardNumber()).isEqualTo(cardNumber);
        assertThat(creditcardProviderData.getCardExpiryDate()).isEqualTo(expiryDate);
        assertThat(creditcardProviderData.getCardBrand()).isEqualTo(cardBrand);
        assertThat(creditcardProviderData.getOrderDescription()).isEqualTo(description);
        assertThat(originalSalesOrder.getCustomerEmail()).isEqualTo("test@test.mail");
    }

    @Test
    void testUpdateSalesOrderByOrderJsonThrowsAnException(){

        // Prepare sales order
        var orderNumber = "872634243";
        var originalSalesOrder = getSalesOrder(readResource("examples/testmessage.json"));
        updateOriginalSalesOrder(orderNumber, originalSalesOrder);

        // Prepare new order json
        var orderJson = getOrderJson(orderNumber, true);

        assertThatThrownBy(() -> salesOrderService.updateSalesOrderByOrderJson(originalSalesOrder, orderJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order does not contain a valid payment type. Order number: " +
                                orderNumber);
    }

    @NotNull
    private Order getOrderJson(String orderNumber, boolean addInvalidPaymentType) {
        String rawMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        var orderJson = getSalesOrder(rawMessage).getLatestJson();
        orderJson.getOrderHeader().setOrderNumber(orderNumber);
        orderJson.getOrderHeader().setOrderGroupId(orderNumber);

        if (addInvalidPaymentType) {
            orderJson.getOrderHeader().setPayments(List.of(
                    Payments.builder().type(paypalType).build(),
                    Payments.builder().type(creditcardType).build(),
                    Payments.builder()
                            .type("voucher")
                            .value(BigDecimal.valueOf(50.99))
                            .paymentProviderData(PaymentProviderData.builder()
                                    .code("ABC123")
                                    .promotionIdentifier("string")
                                    .build())
                            .build()
            ));
        } else {
            orderJson.getOrderHeader().setPayments(List.of(
                    Payments.builder()
                            .type(paypalType)
                            .paymentTransactionId(UUID.randomUUID())
                            .build(),
                    Payments.builder()
                            .type(creditcardType)
                            .paymentProviderData(PaymentProviderData.builder()
                                    .transactionAmount(BigDecimal.valueOf(180.99))
                                    .paymentProviderStatus("DONE")
                                    .build())
                            .paymentTransactionId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                            .build()));
        }
        return orderJson;
    }

    private void updateOriginalSalesOrder(String orderNumber, SalesOrder originalSalesOrder) {
        originalSalesOrder.setOrderNumber(orderNumber);
        originalSalesOrder.getLatestJson().getOrderHeader().getPayments()
                .add(Payments.builder()
                        .type(creditcardType)
                        .paymentTransactionId(uuid)
                        .paymentProviderData(PaymentProviderData.builder()
                                .externalId(externalId)
                                .transactionAmount(transactionAmount)
                                .paymentProviderTransactionId(transactionId)
                                .paymentProviderPaymentId(externalId)
                                .paymentProviderStatus(status)
                                .paymentProviderOrderDescription(description)
                                .paymentProviderCode(providerCode)
                                .pseudoCardNumber(cardNumber)
                                .cardExpiryDate(expiryDate)
                                .cardBrand(cardBrand)
                                .orderDescription(description)
                                .build())
                        .build());
    }

    private PaymentProviderData getPaypalPaymentProviderData(SalesOrder originalSalesOrder) {
        var providerData =
                originalSalesOrder.getLatestJson().getOrderHeader().getPayments().get(0).getPaymentProviderData();
        return PaymentProviderData.builder()
                .externalId(providerData.getExternalId())
                .transactionAmount(providerData.getTransactionAmount())
                .senderFirstName(providerData.getSenderFirstName())
                .senderLastName(providerData.getSenderLastName())
                .externalTransactionId(providerData.getExternalTransactionId())
                .externalPaymentStatus(providerData.getExternalPaymentStatus())
                .billingAgreement(providerData.getBillingAgreement())
                .build();
    }

}