package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
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

        String rawEventMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");
        rawEventMessage = rawEventMessage.replace("InvoiceNumber\\\": \\\"10", "InvoiceNumber\\\": \\\"11111");
        var invoiceMsg = getInvoiceMsg(rawEventMessage);
        var orderNumber = invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var newOrderNumber = createOrderNumberInSOH(orderNumber, invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());

        SalesOrder salesOrder = createSubsequentSalesOrder(orderNumber, "10");
        salesOrder.setInvoiceEvent(invoiceMsg);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(orderUtil.createOrderNumberInSOH(any(), any())).thenReturn(newOrderNumber);

        migrationInvoiceService.handleMigrationSubsequentOrder(invoiceMsg, salesOrder);

        verify(snsPublishService).publishMigrationOrderCreated(newOrderNumber);
        var event = EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(invoiceMsg);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(event);
    }

    @Test
    void testHandleMigrationCoreSalesInvoiceCreatedNewSubsequentOrder() {

        String rawEventMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");
        var invoiceMsg = getInvoiceMsg(rawEventMessage);
        var orderNumber = invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getOrderNumber();
        var newOrderNumber = createOrderNumberInSOH(orderNumber, invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
        when(orderUtil.createOrderNumberInSOH(any(), any())).thenReturn(newOrderNumber);

        SalesOrder salesOrder = createSubsequentSalesOrder(orderNumber, "");
        salesOrder.setInvoiceEvent(invoiceMsg);

        migrationInvoiceService.handleMigrationSubsequentOrder(invoiceMsg, salesOrder);

        verify(snsPublishService, never()).publishMigrationOrderCreated(newOrderNumber);
        var event = EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(invoiceMsg);
        verify(snsPublishService).publishCoreInvoiceReceivedEvent(event);
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    public static String readResource(String path) {
        return Files.readString(Paths.get(
                Objects.requireNonNull(SalesOrderUtil.class.getClassLoader().getResource(path))
                        .toURI()));
    }

    @SneakyThrows(JsonProcessingException.class)
    public static CoreSalesInvoiceCreatedMessage getInvoiceMsg(String rawMessage) {
        ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        return objectMapper.readValue(body, CoreSalesInvoiceCreatedMessage.class);
    }

    public static String createOrderNumberInSOH(String orderNumber, String reference) {
        return orderNumber + ORDER_NUMBER_SEPARATOR + reference;
    }

    private SalesOrder createSubsequentSalesOrder(String orderNumber, String invoiceNumber) {
        String rawOrderMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(rawOrderMessage);
        order.getOrderHeader().setOrderNumber(orderNumber + invoiceNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        return getSalesOrder(order);
    }
}