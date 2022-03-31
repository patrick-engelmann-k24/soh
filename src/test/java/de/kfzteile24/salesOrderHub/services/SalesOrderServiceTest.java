package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.SubsequentDeliveryMessage;
import de.kfzteile24.salesOrderHub.dto.sns.subsequent.SubsequentDeliveryItem;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.readResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @InjectMocks
    private SalesOrderService salesOrderService;

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
                        .build(),
                SalesOrderInvoice.builder()
                        .orderNumber(salesOrder.getOrderNumber())
                        .invoiceNumber("2")
                        .build()
        );
        when(salesOrderRepository.countByCustomerEmail(any())).thenReturn(1L);// 1 > 0
        when(invoiceService.getInvoicesByOrderNumber(salesOrder.getOrderNumber())).thenReturn(existingInvoices);
        when(salesOrderRepository.save(any())).thenReturn(salesOrder);

        SalesOrder createdSalesOrder = salesOrderService.createSalesOrder(salesOrder);

        assertThat(createdSalesOrder.isRecurringOrder()).isTrue();
        assertThat(createdSalesOrder.getLatestJson()).isNotNull();
        assertThat(createdSalesOrder.getCustomerEmail()).isEqualTo("test@kfzteile24.de");
        assertThat(createdSalesOrder.getSalesChannel()).isEqualTo("www-k24-at");
        assertThat(createdSalesOrder.getOrderNumber()).isEqualTo("514000016");
        assertThat(createdSalesOrder.getOrderGroupId()).isEqualTo("514000016");//The orderNumber should be used to fill the group Id with the same number, since it was missing in the Order JSON.

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
    void createSalesOrderForSubsequentDelivery() {
        // Prepare sales order
        String rawMessage =  readResource("examples/testmessage.json");
        var salesOrder = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        String newOrderNumber = "22222";
        OrderRows orderRow = salesOrder.getLatestJson().getOrderRows().get(0);

        // Prepare sub-sequent delivery note obj
        var subsequentDeliveryMessage = createSubsequentDeliveryNote(
                salesOrder.getOrderNumber(),
                orderRow.getSku());

        // Mock services
        when(salesOrderRepository.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(invoiceService.getInvoicesByOrderNumber(any())).thenReturn(Set.of());
        when(orderUtil.createOrderFromOriginalSalesOrder(any(), any(), any())).thenReturn(orderRow);
        when(salesOrderRepository.save(any())).thenAnswer(new Answer<SalesOrder>() {
            @Override
            public SalesOrder answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArgument(0);
            }
        });

        // Establish some updates before the test in order to see the change
        salesOrder.setOrderGroupId("33333"); //set order group id to a different value to observe the change
        GrandTotalTaxes actualGrandTotalTax = GrandTotalTaxes.builder()
                .rate(BigDecimal.TEN)
                .value(BigDecimal.TEN)
                .build();
        salesOrder.getLatestJson().getOrderHeader().getTotals().setGrandTotalTaxes(List.of(actualGrandTotalTax));

        var createdSalesOrder = salesOrderService.createSalesOrderForSubsequentDelivery(
                subsequentDeliveryMessage,
                newOrderNumber);

        assertThat(createdSalesOrder.getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(createdSalesOrder.getOrderGroupId()).isEqualTo(subsequentDeliveryMessage.getOrderNumber());
        assertThat(createdSalesOrder.getOriginalOrder()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isEqualTo(Platform.SOH);
        assertThat(createdSalesOrder.getId()).isNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderRows()).hasSize(1);
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGoodsTotalGross()).isEqualTo(orderRow.getSumValues().getGoodsValueGross());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGoodsTotalNet()).isEqualTo(orderRow.getSumValues().getGoodsValueNet());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getTotalDiscountGross()).isEqualTo(orderRow.getSumValues().getDiscountGross());
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGrandTotalGross()).isEqualTo(orderRow.getSumValues().getGoodsValueGross().subtract(orderRow.getSumValues().getDiscountGross()));
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getTotals().getGrandTotalTaxes()).isEqualTo(
                List.of(GrandTotalTaxes.builder()
                        .rate(orderRow.getTaxRate())
                        .value(new BigDecimal("33.50"))
                        .build())
        );
    }

    private SubsequentDeliveryMessage createSubsequentDeliveryNote(String orderNumber, String sku) {
        BigDecimal quantity = BigDecimal.valueOf(1L);
        String subsequentDeliveryNumber = "987654321";
        SubsequentDeliveryItem item = new SubsequentDeliveryItem();
        item.setSku(sku);
        item.setQuantity(quantity);
        item.setUnitPriceGross(BigDecimal.TEN);
        item.setSalesPriceGross(BigDecimal.TEN.multiply(quantity));
        item.setTaxRate(BigDecimal.TEN);
        SubsequentDeliveryMessage subsequent = new SubsequentDeliveryMessage();
        subsequent.setOrderNumber(orderNumber);
        subsequent.setItems(List.of(item));
        subsequent.setSubsequentDeliveryNoteNumber(subsequentDeliveryNumber);
        return subsequent;
    }

    private void updateRowIsCancelledFieldAsTrue(SalesOrder salesOrder) {
        var order = salesOrder.getLatestJson();
        order.getOrderRows().forEach(r -> r.setIsCancelled(true));
        salesOrder.setLatestJson(order);
        salesOrder.setOriginalOrder(order);
    }

    @Test
    public void testGetOrderNumberListByOrderGroupIdForMultipleSalesOrdersHavingRightSku(){
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

        when(salesOrderRepository.findAllByOrderGroupId(any())).thenReturn(Optional.of(salesOrderList));

        var orderNumberList = salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder2.getLatestJson().getOrderRows().get(0).getSku());

        assertThat(orderNumberList).contains("1111");
        assertThat(orderNumberList).contains("2222");
        assertThat(orderNumberList).doesNotContain("3333");
        assertThat(orderNumberList).doesNotContain("4444");
    }

    @Test
    public void testGetOrderNumberListByOrderGroupIdForNoneSalesOrderHavingRightSku(){
        var salesOrderList = new ArrayList<SalesOrder>();
        String rawMessage =  readResource("examples/ecpOrderMessage.json");

        // create sales order to be rejected due to different sku number
        var salesOrder = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        salesOrder.setOrderGroupId("123");
        salesOrder.getLatestJson().getOrderRows().get(0).setSku("98765432");
        salesOrderList.add(salesOrder); // 4 orders

        when(salesOrderRepository.findAllByOrderGroupId(any())).thenReturn(Optional.of(salesOrderList));

        assertThatThrownBy(() -> salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder.getLatestJson().getOrderRows().get(0).getSku()))
                .isInstanceOf(SalesOrderNotFoundCustomException.class)
                .hasMessageContaining("Sales order not found for the given order group id",
                        salesOrder.getOrderGroupId());
    }

    @Test
    public void testGetOrderNumberListByOrderGroupIdForNoneSalesOrderInGroupId(){
        var salesOrderList = new ArrayList<SalesOrder>();
        String rawMessage =  readResource("examples/ecpOrderMessage.json");

        // create sales order to be rejected due to different sku number
        var salesOrder = getSalesOrder(rawMessage);
        updateRowIsCancelledFieldAsTrue(salesOrder); //In order to observe change
        salesOrder.getLatestJson().getOrderRows().get(0).setSku("98765432");
        salesOrderList.add(salesOrder); // 4 orders

        when(salesOrderRepository.findAllByOrderGroupId(any())).thenReturn(Optional.of(salesOrderList));

        assertThatThrownBy(() -> salesOrderService.getOrderNumberListByOrderGroupIdAndFilterNotCancelled(
                "123",
                salesOrder.getLatestJson().getOrderRows().get(0).getSku()))
                .isInstanceOf(SalesOrderNotFoundCustomException.class)
                .hasMessageContaining("Sales order not found for the given order group id",
                        salesOrder.getOrderGroupId(),
                        "order row sku number",
                        salesOrder.getLatestJson().getOrderRows().get(0).getSku());
    }

}