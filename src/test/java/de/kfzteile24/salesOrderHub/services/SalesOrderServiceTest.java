package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesFinancialDocumentLine;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.CustomerType;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.PaymentProviderData;
import de.kfzteile24.soh.order.dto.Payments;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.copyOrderJson;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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
    private SubsequentSalesOrderCreationHelper subsequentSalesOrderCreationHelper;

    @Mock
    private DropshipmentOrderRowService dropshipmentOrderRowService;

    @Spy
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
        final var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));

        when(salesOrderRepository.countByCustomerEmail(salesOrder.getCustomerEmail())).thenReturn(orderCount);

        final boolean isRecurring = salesOrderService.isRecurringOrder(salesOrder);
        assertThat(isRecurring).isEqualTo(expectedResult);
    }

    @Test
    void createSalesOrder() {
        var message = getObjectByResource("ecpOrderMessage.json", Order.class);
        SalesOrder salesOrder = getSalesOrder(message);
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

        verify(invoiceService).getInvoicesByOrderNumber(salesOrder.getOrderNumber());
        existingInvoices
                .forEach(invoice -> verify(invoiceService).addSalesOrderToInvoice(salesOrder, invoice));

        verify(auditLogRepository).save(any());

    }

    @Test
    void savingAnOrderAlsoInsertsAnAuditLogEntry() {
        var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        salesOrder.setId(UUID.randomUUID());
        when(salesOrderRepository.save(salesOrder)).thenReturn(salesOrder);

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
        var message = getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        String newOrderNumber = "22222";
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createCoreSalesInvoiceCreatedMessage(
                salesOrder.getOrderNumber(),
                orderRow.getSku());

        // Expected values
        var invoiceNumber = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber();
        var expectedSalesOrder = getSalesOrder(message);
        expectedSalesOrder.getLatestJson().getOrderHeader().setOrderGroupId(salesOrder.getOrderNumber());
        expectedSalesOrder.getLatestJson().getOrderHeader().setOrderNumber(newOrderNumber);
        expectedSalesOrder.getLatestJson().getOrderHeader().setPlatform(Platform.SOH);
        expectedSalesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceNumber);

        // Mock services
        when(invoiceService.getInvoicesByOrderNumber(any())).thenReturn(Set.of());
        when(orderUtil.getLastRowKey(eq(salesOrder))).thenReturn(3);
        when(orderUtil.createNewOrderRow(any(), any(), any())).thenReturn(orderRow);
        when(orderUtil.removeInvalidGrandTotalTaxes(any())).thenReturn(null);
        when(salesOrderRepository.save(any())).thenAnswer((Answer<SalesOrder>) invocation -> invocation.getArgument(0));
        when(subsequentSalesOrderCreationHelper.createOrderHeader(any(), anyString(), anyString())).thenReturn(expectedSalesOrder.getLatestJson().getOrderHeader());

        var createdSalesOrder = salesOrderService.createSalesOrderForInvoice(
                invoiceCreatedMessage,
                salesOrder,
                newOrderNumber);

        assertThat(createdSalesOrder.getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(createdSalesOrder.getOrderGroupId()).isEqualTo("524001248");
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId()).isEqualTo("524001248");
        assertThat(createdSalesOrder.getCustomerEmail()).isEqualTo(expectedSalesOrder.getLatestJson().getOrderHeader().getCustomer().getCustomerEmail());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isEqualTo(Platform.SOH);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.valueOf(8.7));
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.valueOf(8));
        assertThat(createdSalesOrder.getLatestJson().getOrderRows().get(0).getSku()).isEqualTo(orderRow.getSku());
    }


    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for No Shipping Cost LInes")
    void testFullyMatchedWithOriginalOrderNoShippingCostLines() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage =
                createFullyMatchedItemsMessage(salesOrder, "2010-10183", BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.19));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for No Shipping Cost LInes")
    void testFullyMatchedWhenDropshuniipmentOrderAndCancelledOrderInOrderGroupId() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder1 = getSalesOrder(message);
        assertEquals(2, salesOrder1.getLatestJson().getOrderRows().size());
        var orderGroupId = salesOrder1.getOrderGroupId();

        // Prepare cancelled sales order
        var message2 = getObjectByResource("testmessageCancelled.json", Order.class);
        var salesOrder2 = getSalesOrder(message2);
        salesOrder2.setCancelled(true);

        // Prepare DS order
        var messageDropshipment = getObjectByResource("testmessageDropshipment.json", Order.class);
        var salesOrder3 = getSalesOrder(messageDropshipment);

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage =
                createFullyMatchedItemsMessage(salesOrder1, "2010-10183", BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.19));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();

        when(salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(orderGroupId))
                .thenReturn(List.of(salesOrder1, salesOrder2, salesOrder3));
        when(orderUtil.isDropshipmentOrder(eq(message))).thenReturn(false);
        when(orderUtil.isDropshipmentOrder(eq(message2))).thenReturn(false);
        when(orderUtil.isDropshipmentOrder(eq(messageDropshipment))).thenReturn(true);

        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder1, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for Unknown Sku Shipping Cost LIne")
    void testFullyMatchedWithOriginalOrderUnknownSkuShippingCostLine() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage =
                createFullyMatchedItemsMessage(salesOrder, "test", BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.19));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }


    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for Existing Sku Shipping Cost LIne")
    void testFullyMatchedWithOriginalOrderExistingSkuShippingCostLine() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder,
                orderRow.getSku(), BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.19));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertTrue(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is NOT Fully Matched With Original Order for NOT NULL And NULL ShippingCostNet")
    void testNotFullyMatchedWithOriginalOrderExistingSkuShippingCostLine() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
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
    @DisplayName("Test That Invoice is NOT Fully Matched With Original Order if first order row is cancelled")
    void testNotFullyMatchedWithOriginalOrderIfFirstOrderRowIsCancelled() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);
        orderRow.setIsCancelled(true);

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder,
                orderRow.getSku(), BigDecimal.valueOf(1.00), BigDecimal.valueOf(1.19));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertFalse(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test That Invoice is Fully Matched With Original Order for NOT NULL Matching ShippingCostNet")
    void testFullyMatchedWithOriginalOrderNotNullShippingCostNet() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
        assertEquals(2, salesOrder.getLatestJson().getOrderRows().size());
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);
        salesOrder.getLatestJson().getOrderHeader().getTotals().setShippingCostNet(BigDecimal.valueOf(0.1));

        // Prepare sub-sequent delivery note obj
        var invoiceCreatedMessage = createFullyMatchedItemsMessage(salesOrder,
                orderRow.getSku(), BigDecimal.valueOf(0.1), BigDecimal.valueOf(0.3));

        CoreSalesInvoiceHeader salesInvoiceHeader = invoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        assertFalse(salesOrderService.isFullyMatchedWithOriginalOrder(salesOrder, salesInvoiceHeader.getInvoiceLines()));
    }

    @Test
    @DisplayName("Test isFullyInvoiced for Dropshipment with null invoice")
    void testFullyInvoicedWithNullInvoiceDataThrowsAnException() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);

        // Prepare invoice data
        InvoiceData invoiceData = InvoiceData.builder().orderNumber(salesOrder.getOrderNumber()).build();

        when(salesOrderRepository.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .thenReturn(Optional.of(salesOrder));
        assertThatThrownBy(() -> salesOrderService.isFullyInvoiced(invoiceData))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order row in invoice data is null or empty.");

    }
    @Test
    @DisplayName("Test isFullyInvoiced for Dropshipment with empty invoice")
    void testFullyInvoicedWithEmptyInvoiceDataThrowsAnException() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);

        // Prepare invoice data
        InvoiceData invoiceData = InvoiceData.builder()
                .orderNumber(salesOrder.getOrderNumber()).orderRows(new ArrayList<>()).build();

        when(salesOrderRepository.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .thenReturn(Optional.of(salesOrder));
        assertThatThrownBy(() -> salesOrderService.isFullyInvoiced(invoiceData))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Order row in invoice data is null or empty.");

    }

    @Test
    @DisplayName("Test isFullyInvoiced for Dropshipment with order rows not matched")
    void testFullyInvoicedWithOrderRowNotMatched() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);

        // Prepare invoice data
        final var invoiceNumber = "456";
        final var skuList = List.of("1440-47378");
        val quantities = List.of(1);
        final var invoiceData =
                InvoiceData.builder().invoiceNumber(invoiceNumber).orderNumber(salesOrder.getOrderNumber())
                        .orderRows(skuList)
                        .quantities(quantities).build();

        when(salesOrderRepository.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .thenReturn(Optional.of(salesOrder));
        assertFalse(salesOrderService.isFullyInvoiced(invoiceData));

    }
    @Test
    @DisplayName("Test isFullyInvoiced for Dropshipment with quantity not matched")
    void testFullyInvoicedWithQuantityNotMatched() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);

        // Prepare invoice data
        final var invoiceNumber = "456";
        final var skuList = List.of("1440-47378", "2010-10183");
        val quantities = List.of(1, 0);
        final var invoiceData =
                InvoiceData.builder().invoiceNumber(invoiceNumber).orderNumber(salesOrder.getOrderNumber())
                        .orderRows(skuList)
                        .quantities(quantities).build();

        when(salesOrderRepository.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .thenReturn(Optional.of(salesOrder));
        assertFalse(salesOrderService.isFullyInvoiced(invoiceData));

    }

    @Test
    @DisplayName("Test isFullyInvoiced for Dropshipment with fully matched order rows and quantity")
    void testFullyInvoicedWithFullInvoice() {
        // Prepare sales order
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);

        // Prepare invoice data
        final var invoiceNumber = "456";
        final var skuList = List.of("1440-47378", "2010-10183");
        val quantities = List.of(1, 1);
        final var invoiceData =
                InvoiceData.builder().invoiceNumber(invoiceNumber).orderNumber(salesOrder.getOrderNumber())
                        .orderRows(skuList)
                        .quantities(quantities).build();

        when(salesOrderRepository.getOrderByOrderNumber(salesOrder.getOrderNumber()))
                .thenReturn(Optional.of(salesOrder));
        assertTrue(salesOrderService.isFullyInvoiced(invoiceData));

    }

    private CoreSalesInvoiceCreatedMessage createCoreSalesInvoiceCreatedMessage(String orderNumber, String sku) {
        var item = CoreSalesFinancialDocumentLine.builder()
                .itemNumber(sku)
                .quantity(BigDecimal.valueOf(1L))
                .unitNetAmount(BigDecimal.valueOf(9))
                .lineNetAmount(BigDecimal.valueOf(9))
                .unitGrossAmount(BigDecimal.valueOf(9.8))
                .lineGrossAmount(BigDecimal.valueOf(9.8))
                .taxRate(BigDecimal.TEN)
                .isShippingCost(false)
                .build();
        var shipping = CoreSalesFinancialDocumentLine.builder()
                .itemNumber("shipping")
                .quantity(BigDecimal.valueOf(1L))
                .unitNetAmount(BigDecimal.valueOf(8))
                .lineNetAmount(BigDecimal.valueOf(8))
                .unitGrossAmount(BigDecimal.valueOf(8.7))
                .lineGrossAmount(BigDecimal.valueOf(8.7))
                .taxRate(BigDecimal.TEN)
                .isShippingCost(true)
                .build();

        CoreSalesInvoiceHeader coreSalesInvoiceHeader = new CoreSalesInvoiceHeader();
        coreSalesInvoiceHeader.setOrderNumber(orderNumber);
        coreSalesInvoiceHeader.setInvoiceNumber("50");
        coreSalesInvoiceHeader.setInvoiceLines(List.of(item, shipping));

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
        var message = getObjectByResource("ecpOrderMessage.json", Order.class);

        // create multiple sales orders to be accepted and returned as result
        var salesOrder = getSalesOrder(message);
        salesOrder.setOrderNumber("1111");
        salesOrder.setOrderGroupId("123");
        salesOrderList.add(salesOrder); // 1 order
        var salesOrder2 = getSalesOrder(copyOrderJson(message));
        salesOrder2.setOrderNumber("2222");
        salesOrder2.setOrderGroupId("123");
        salesOrderList.add(salesOrder2); // 2 orders

        // create sales order to be rejected due to is cancelled field in row
        var salesOrderToBeRejected = getSalesOrder(copyOrderJson(message));
        updateRowIsCancelledFieldAsTrue(salesOrderToBeRejected); //In order to observe change
        salesOrderToBeRejected.setOrderNumber("3333");
        salesOrderToBeRejected.setOrderGroupId("123");
        salesOrderList.add(salesOrderToBeRejected); // 3 orders

        // create sales order to be rejected due to different sku number
        var salesOrderToBeRejected2 = getSalesOrder(copyOrderJson(message));
        updateRowIsCancelledFieldAsTrue(salesOrderToBeRejected2); //In order to observe change
        salesOrderToBeRejected2.setOrderNumber("4444");
        salesOrderToBeRejected2.setOrderGroupId("123");
        salesOrderToBeRejected2.getLatestJson().getOrderRows().get(0).setSku("98765432");
        salesOrderList.add(salesOrderToBeRejected2); // 4 orders

        when(salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(any())).thenReturn(salesOrderList);

        var orderNumberList = salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder2.getLatestJson().getOrderRows().get(0).getSku());

        assertThat(orderNumberList)
                .containsExactlyInAnyOrder("1111", "2222")
                .doesNotContain("3333", "4444");
    }

    @Test
    void testGetOrderNumberListByOrderGroupIdForNoneSalesOrderHavingRightSku(){
        var salesOrderList = new ArrayList<SalesOrder>();
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
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
        var message =  getObjectByResource("testmessage.json", Order.class);
        var salesOrder = getSalesOrder(message);
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
        var originalSalesOrder = getSalesOrder(getObjectByResource("testmessage.json", Order.class));
        var originalProviderData = getPaypalPaymentProviderData(originalSalesOrder);
        updateOriginalSalesOrder(orderNumber, originalSalesOrder);

        // Prepare new order json
        var orderJson = getOrderJson(orderNumber, false);
        var originalOrderJson = getOrderJson(orderNumber, false);

        when(salesOrderRepository.save(any())).thenAnswer((Answer<SalesOrder>) invocation -> invocation.getArgument(0));

        salesOrderService.enrichSalesOrder(originalSalesOrder, orderJson, originalOrderJson);
        salesOrderService.save(originalSalesOrder, MIGRATION_SALES_ORDER_RECEIVED);
        var orderHeader = originalSalesOrder.getLatestJson().getOrderHeader();
        var paypalPayment = orderHeader.getPayments().stream().filter(p -> p.getType().equals(paypalType)).findFirst().orElseThrow();
        var paypalProviderData = paypalPayment.getPaymentProviderData();
        var creditcardPayment = orderHeader.getPayments().stream().filter(p -> p.getType().equals(creditcardType)).findFirst().orElseThrow();
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
        var originalSalesOrder = getSalesOrder(getObjectByResource("testmessage.json", Order.class));
        updateOriginalSalesOrder(orderNumber, originalSalesOrder);

        // Prepare new order json
        var orderJson = getOrderJson(orderNumber, true);

        assertThatThrownBy(() -> salesOrderService.enrichSalesOrder(originalSalesOrder, orderJson, orderJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order does not contain a valid payment type. Order number: " +
                                orderNumber);
    }

    @ParameterizedTest
    @MethodSource("provideParamsForUpdateCustomSegmentTest")
    void testUpdateCustomSegment(List<String> existingCustomerSegments,
                                 CustomerType customerType,
                                 String shippingType,
                                 String[] expectedCustomerSegments,
                                 boolean isExistingCustomerSegmentsNull) {
        var salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));

        var order = (Order) salesOrder.getOriginalOrder();
        order.getOrderHeader().getCustomer().setCustomerSegment(existingCustomerSegments);
        order.getOrderHeader().getCustomer().setCustomerType(customerType);
        order.getOrderRows().get(0).setShippingType(shippingType);

        salesOrderService.updateCustomSegment(order);

        if (isExistingCustomerSegmentsNull) {
            assertThat(salesOrder.getLatestJson().getOrderHeader().getCustomer().getCustomerSegment()).isNull();
        } else {
            assertThat(salesOrder.getLatestJson().getOrderHeader().getCustomer().getCustomerSegment())
                    .containsExactlyInAnyOrder(expectedCustomerSegments);
        }
    }

    private static Stream<Arguments> provideParamsForUpdateCustomSegmentTest() {
        return Stream.of(
                Arguments.of(null, CustomerType.BUSINESS, "notRelevant", new String[] {"b2b"}, false),
                Arguments.of(new ArrayList<>(), CustomerType.BUSINESS, "notRelevant", new String[] {"b2b"}, false),
                Arguments.of(null, CustomerType.UNKNOWN, "direct delivery", new String[] {"direct_delivery"}, false),
                Arguments.of(new ArrayList<>(), CustomerType.UNKNOWN, "direct delivery", new String[] {"direct_delivery"}, false),
                Arguments.of(null, CustomerType.BUSINESS, "direct delivery", new String[] {"b2b", "direct_delivery"}, false),
                Arguments.of(new ArrayList<>(), CustomerType.BUSINESS, "direct delivery", new String[] {"b2b", "direct_delivery"}, false),
                Arguments.of(null, null, "direct delivery", new String[] {"direct_delivery"}, false),
                Arguments.of(new ArrayList<>(), null, "direct delivery", new String[] {"direct_delivery"}, false),
                Arguments.of(
                        new ArrayList<>(){{
                            add("anyCustomerSegment");
                        }}, CustomerType.BUSINESS, "direct delivery", new String[] {"b2b", "direct_delivery", "anyCustomerSegment"}, false),
                Arguments.of(
                        new ArrayList<>(){{
                            add("anyCustomerSegment");
                        }}, CustomerType.UNKNOWN, "direct delivery", new String[] {"anyCustomerSegment", "direct_delivery"}, false),
                Arguments.of(null, CustomerType.UNKNOWN, "notRelevant", new String[] {}, true),
                Arguments.of(new ArrayList<>(), CustomerType.UNKNOWN, "notRelevant", new String[] {}, false),
                Arguments.of(
                        new ArrayList<>(){{
                            add("anyCustomerSegment");
                        }}, CustomerType.UNKNOWN, "notRelevant", new String[] {"anyCustomerSegment"}, false),
                Arguments.of(
                        new ArrayList<>(){{
                            add("anyCustomerSegment");
                        }}, CustomerType.UNKNOWN, "notRelevant", new String[] {"anyCustomerSegment"}, false),
                Arguments.of(
                        new ArrayList<>(){{
                            add("b2b");
                            add("direct_delivery");
                        }}, CustomerType.UNKNOWN, "notRelevant", new String[] {"b2b", "direct_delivery"}, false)
        );
    }

    @NotNull
    private Order getOrderJson(String orderNumber, boolean addInvalidPaymentType) {
        var message = getObjectByResource("ecpOrderMessageWithTwoRows.json", Order.class);
        var orderJson = getSalesOrder(message).getLatestJson();
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

    @Test
    void testCancelOrder(){
        // Prepare input
        var orderNumber = UUID.randomUUID().toString();
        var salesOrder = getSalesOrder(getObjectByResource("testmessage.json", Order.class));
        doReturn(Optional.of(salesOrder)).when(salesOrderService).getOrderByOrderNumber(orderNumber);
        doReturn(null).when(salesOrderService).save(any(), any());

        // Prepare expected value
        var expected = getSalesOrder(getObjectByResource("testmessage.json", Order.class));
        expected.setCancelled(true);

        salesOrderService.cancelOrder(orderNumber);
        verify(salesOrderService).save(eq(expected), eq(ORDER_CANCELLED));
    }

    @Test
    void testGetNextOrderNumberIndexCounterForOneOrderInGroup(){
        var orderNumber = RandomStringUtils.randomNumeric(9);
        Order order = getObjectByResource("testmessage.json", Order.class);
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        var salesOrder = getSalesOrder(order);
        var orderGroupId = salesOrder.getOrderGroupId();
        doReturn(List.of(orderNumber))
                .when(salesOrderRepository).findOrderNumberByOrderGroupId(orderGroupId);

        int counter = salesOrderService.getNextOrderNumberIndexCounter(orderGroupId);
        assertThat(counter).isEqualTo(1);
    }

    @Test
    void testGetNextOrderNumberIndexCounterForMultipleOrdersInGroup(){
        var orderNumber = RandomStringUtils.randomNumeric(9);
        Order order = getObjectByResource("testmessage.json", Order.class);
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        var salesOrder = getSalesOrder(order);
        var orderGroupId = salesOrder.getOrderGroupId();
        doReturn(List.of(orderNumber, orderNumber + "-1", orderNumber + "-2"))
                .when(salesOrderRepository).findOrderNumberByOrderGroupId(orderGroupId);

        int counter = salesOrderService.getNextOrderNumberIndexCounter(orderGroupId);
        assertThat(counter).isEqualTo(3);
    }

    @Test
    void testGetNextOrderNumberIndexCounterForNoOrderInGroup(){
        var orderGroupId = RandomStringUtils.randomNumeric(9);
        doReturn(new ArrayList<>())
                .when(salesOrderRepository).findOrderNumberByOrderGroupId(orderGroupId);

        assertThatThrownBy(() ->salesOrderService.getNextOrderNumberIndexCounter(orderGroupId))
                .isInstanceOf(SalesOrderNotFoundCustomException.class)
                .hasMessage("Sales order not found for the given order group id " + orderGroupId + " ");
    }

}