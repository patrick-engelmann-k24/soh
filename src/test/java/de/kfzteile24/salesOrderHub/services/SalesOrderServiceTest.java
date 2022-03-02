package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.SubsequentDeliveryMessage;
import de.kfzteile24.salesOrderHub.dto.sns.subsequent.SubsequentDeliveryItem;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.soh.order.dto.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
import static org.mockito.ArgumentMatchers.*;
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

    @InjectMocks
    private SalesOrderService salesOrderService;

    @ParameterizedTest
    @MethodSource("provideParamsForRecurringOrderTest")
    public void recurringOrdersAreIdentifiedCorrectly(long orderCount, boolean expectedResult) {
        final var salesOrder = getSalesOrder(readResource("examples/ecpOrderMessage.json"));

        when(salesOrderRepository.countByCustomerEmail(salesOrder.getCustomerEmail())).thenReturn(orderCount);

        final boolean isRecurring = salesOrderService.isRecurringOrder(salesOrder);
        assertThat(isRecurring).isEqualTo(expectedResult);
    }

    @Test
    public void createSalesOrder(){
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
    public void savingAnOrderAlsoInsertsAnAuditLogEntry() {
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
    public void createSalesOrderForSubsequentDelivery(){
        String rawMessage =  readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        BigDecimal quantity = BigDecimal.valueOf(1L);
        String sku = salesOrder.getLatestJson().getOrderRows().get(0).getSku();
        String subsequentDeliveryNumber = "987654321";
        SubsequentDeliveryItem item = new SubsequentDeliveryItem();
        item.setSku(sku);
        item.setQuantity(quantity);
        SubsequentDeliveryMessage subsequent = new SubsequentDeliveryMessage();
        subsequent.setOrderNumber(salesOrder.getOrderNumber());
        subsequent.setItems(List.of(item));
        subsequent.setSubsequentDeliveryNoteNumber(subsequentDeliveryNumber);
        salesOrder.setOrderGroupId("33333"); //set order group id to a different value to observe the change
        when(salesOrderRepository.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));

        SalesOrder createdSalesOrder = salesOrderService.createSalesOrderForSubsequentDelivery(subsequent, "22222");

        assertThat(createdSalesOrder.getOrderNumber()).isEqualTo("22222");
        assertThat(createdSalesOrder.getOrderGroupId()).isEqualTo("514000016");
        assertThat(createdSalesOrder.getSalesChannel()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getSalesChannel());
        assertThat(createdSalesOrder.getCustomerEmail()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getCustomer().getCustomerEmail());
        assertThat(createdSalesOrder.getOriginalOrder()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isNotNull();
        assertThat(createdSalesOrder.getLatestJson().getOrderHeader().getPlatform()).isEqualTo(Platform.SOH);

    }

    @Test
    public void ifNewlyCreatedSalesOrderDoesNotIncludeTheSkuGivenSubsequentDeliveryItShouldThrowException(){
        String rawMessage =  readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        BigDecimal quantity = BigDecimal.valueOf(1L);
        String sku = "44444";
        String subsequentDeliveryNumber = "987654321";
        SubsequentDeliveryItem item = new SubsequentDeliveryItem();
        item.setSku(sku);
        item.setQuantity(quantity);
        SubsequentDeliveryMessage subsequent = new SubsequentDeliveryMessage();
        subsequent.setOrderNumber(salesOrder.getOrderNumber());
        subsequent.setItems(List.of(item));
        subsequent.setSubsequentDeliveryNoteNumber(subsequentDeliveryNumber);
        salesOrder.setOrderGroupId("33333"); //set order group id to a different value to observe the change
        when(salesOrderRepository.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));

        assertThatThrownBy(() -> salesOrderService.createSalesOrderForSubsequentDelivery(subsequent, "22222"))
                .isInstanceOf(NotFoundException.class);
    }

}