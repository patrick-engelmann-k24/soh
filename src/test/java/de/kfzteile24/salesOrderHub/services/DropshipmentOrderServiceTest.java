package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.helper.ReturnOrderHelper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.assertSalesCreditNoteCreatedMessage;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderServiceTest {

    public static final String ANY_INVOICE_NUMBER = RandomStringUtils.randomNumeric(10);

    @InjectMocks
    @Spy
    private DropshipmentOrderService dropshipmentOrderService;

    @Mock
    private SalesOrderService salesOrderService;

    @Mock
    private SalesOrderReturnService salesOrderReturnService;

    @Mock
    private SnsPublishService snsPublishService;

    @Mock
    private InvoiceService invoiceService;

    @Spy
    private ObjectMapperConfig objectMapperConfig;

    @Spy
    private ReturnOrderHelper returnOrderHelper;

    @ParameterizedTest
    @MethodSource("provideArgumentsForIsDropShipmentOrder")
    void testIsDropShipmentOrder(String fulfillment, boolean expected) {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(fulfillment);
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));
        assertThat(dropshipmentOrderService.isDropShipmentOrder(salesOrder.getOrderNumber())).isEqualTo(expected);
    }

    @Test
    @SneakyThrows
    public void testHandleDropshipmentPurchaseOrderReturnConfirmed() {
        String rawMessage = readResource("examples/dropshipmentPurchaseOrderReturnConfirmed.json");
        String body = objectMapperConfig.objectMapper().readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderReturnConfirmedMessage message =
                objectMapperConfig.objectMapper().readValue(body, DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        String orderNumber = message.getSalesOrderNumber();

        String rawSalesOrderMessage = readResource("examples/ecpOrderMessageWithTwoRows.json");
        SalesOrder salesOrder = getSalesOrder(rawSalesOrderMessage);

        when(salesOrderReturnService.createCreditNoteNumber()).thenReturn("2022200002");
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage = returnOrderHelper.buildSalesCreditNoteCreatedMessage(
                message, salesOrder, salesOrderReturnService.createCreditNoteNumber());

        doReturn(salesCreditNoteCreatedMessage).when(dropshipmentOrderService).buildSalesCreditNoteCreatedMessage(eq(message));

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message);

        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber()).isEqualTo("2022200002");
        assertThat(salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber()).isEqualTo(orderNumber);
        assertSalesCreditNoteCreatedMessage(salesCreditNoteCreatedMessage, salesOrder);

        verify(salesOrderReturnService).handleSalesOrderReturn(salesCreditNoteCreatedMessage, DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED, DROPSHIPMENT_ORDER_RETURN_CONFIRMED);
    }

    @Test
    @SneakyThrows
    void testHandleDropshipmentPurchaseOrderReturnNotified() {

        SalesOrder salesOrder = getSalesOrder(readResource("examples/ecpOrderMessage.json"));
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        String rawMessage = readResource("examples/dropshipmentPurchaseOrderReturnNotified.json");
        var sqsMessage = objectMapperConfig.objectMapper().readValue(rawMessage, SqsMessage.class);
        var message =
                objectMapperConfig.objectMapper().readValue(sqsMessage.getBody(),
                        DropshipmentPurchaseOrderReturnNotifiedMessage.class);

        MessageWrapper<DropshipmentPurchaseOrderReturnNotifiedMessage> messageWrapper =
                MessageWrapper.<DropshipmentPurchaseOrderReturnNotifiedMessage>builder()
                        .rawMessage(rawMessage)
                        .message(message)
                        .build();

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnNotified(messageWrapper);

        verify(snsPublishService).publishDropshipmentOrderReturnNotifiedEvent(salesOrder, message);
    }

    @Test
    void testRecreateSalesOrderInvoice() {
        String rawMessage =  readResource("examples/testmessage.json");

        var salesOrder = getSalesOrder(rawMessage);
        assertThat(salesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        assertThat(salesOrder.getInvoiceEvent()).isNull();

        var coreSalesInvoiceCreatedMessage = CoreSalesInvoiceCreatedMessage.builder()
                .salesInvoice(CoreSalesInvoice.builder()
                        .salesInvoiceHeader(CoreSalesInvoiceHeader.builder()
                                .invoiceNumber(ANY_INVOICE_NUMBER)
                                .build())
                        .build())
                .build();

        when(salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber())).thenReturn(Optional.of(salesOrder));
        when(invoiceService.createInvoiceNumber()).thenReturn(ANY_INVOICE_NUMBER);
        when(invoiceService.generateInvoiceMessage(any())).thenReturn(coreSalesInvoiceCreatedMessage);
        when(salesOrderService.save(any(), any())).thenReturn(salesOrder);
        dropshipmentOrderService.recreateSalesOrderInvoice(salesOrder.getOrderNumber());

        verify(salesOrderService).save(argThat(correctlyUpdatedSalesOrder -> {
            assertThat(correctlyUpdatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber())
                    .isEqualTo(ANY_INVOICE_NUMBER);
            assertThat(correctlyUpdatedSalesOrder.getInvoiceEvent()
                    .getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(ANY_INVOICE_NUMBER);
            return true;
        }), eq(DROPSHIPMENT_INVOICE_STORED));
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return FileUtil.readResource(getClass(), path);
    }

    private static Stream<Arguments> provideArgumentsForIsDropShipmentOrder() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(StringUtils.EMPTY, false),
                Arguments.of(K24.getName(), false),
                Arguments.of(DELTICOM.getName(), true)
        );
    }
}