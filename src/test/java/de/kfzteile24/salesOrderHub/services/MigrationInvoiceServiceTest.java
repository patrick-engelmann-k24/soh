package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MigrationInvoiceServiceTest {
    public static final String ORDER_NUMBER_SEPARATOR = "-";

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private OrderUtil orderUtil;

    @Mock
    private SnsPublishService snsPublishService;

    @InjectMocks
    private MigrationInvoiceService migrationInvoiceService;

    @Test
    void testHandleMigrationCoreSalesInvoiceCreatedDuplication() {

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        message.getSalesInvoice().getSalesInvoiceHeader().setInvoiceNumber("11111");

        var orderNumber = message.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var newOrderNumber = createOrderNumberInSOH(orderNumber, message.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());

        SalesOrder salesOrder = createSubsequentSalesOrder(orderNumber, "10");
        salesOrder.setInvoiceEvent(message);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(orderUtil.createOrderNumberInSOH(any(), any())).thenReturn(newOrderNumber);

        migrationInvoiceService.handleMigrationSubsequentOrder(message, salesOrder);

        verify(snsPublishService).publishMigrationOrderCreated(newOrderNumber);
        var event = EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(message);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(event);
    }

    @Test
    void testHandleMigrationCoreSalesInvoiceCreatedNewSubsequentOrder() {

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var orderNumber = message.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var newOrderNumber = createOrderNumberInSOH(orderNumber, message.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
        when(orderUtil.createOrderNumberInSOH(any(), any())).thenReturn(newOrderNumber);

        SalesOrder salesOrder = createSubsequentSalesOrder(orderNumber, "");
        salesOrder.setInvoiceEvent(message);

        migrationInvoiceService.handleMigrationSubsequentOrder(message, salesOrder);

        verify(snsPublishService, never()).publishMigrationOrderCreated(newOrderNumber);
        var event = EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(message);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(event);
    }

    public static String createOrderNumberInSOH(String orderNumber, String reference) {
        return orderNumber + ORDER_NUMBER_SEPARATOR + reference;
    }

    private SalesOrder createSubsequentSalesOrder(String orderNumber, String invoiceNumber) {
        var order = getObjectByResource("ecpOrderMessage.json", Order.class);
        order.getOrderHeader().setOrderNumber(orderNumber + invoiceNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        return getSalesOrder(order);
    }
}