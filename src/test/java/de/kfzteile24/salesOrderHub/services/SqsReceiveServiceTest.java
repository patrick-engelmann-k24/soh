package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.PAYPAL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getCreditNoteMsg;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author vinaya
 */

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnusedPrivateField")
class SqsReceiveServiceTest {

    private static final String ANY_SENDER_ID = RandomStringUtils.randomAlphabetic(10);
    private static final int ANY_RECEIVE_COUNT = RandomUtils.nextInt();

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private SalesOrderReturnService salesOrderReturnService;
    @Mock
    private SalesOrderRowService salesOrderRowService;
    @Mock
    private SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    @Mock
    private CamundaHelper camundaHelper;
    @Mock
    private FeatureFlagConfig featureFlagConfig;
    @Mock
    private SnsPublishService snsPublishService;
    @Mock
    private CreditNoteEventMapper creditNoteEventMapper;
    @InjectMocks
    private SqsReceiveService sqsReceiveService;

    @Test
    void testQueueListenerEcpShopOrders() {
        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);

        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);

        sqsReceiveService.queueListenerEcpShopOrders(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).createSalesOrder(salesOrder);
        verify(salesOrderService, never()).getOrderByOrderNumber(eq(salesOrder.getOrderNumber()));
    }

    @Test
    void testQueueListenerCoreDuplicateShopOrders() {
        //This test case can be removed if no more duplicate items are expected from core publisher.
        var senderId = "Core";
        String rawMessage = readResource("examples/coreOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        salesOrder.setRecurringOrder(false);

        when(salesOrderService.getOrderByOrderNumber("524001240")).thenReturn(Optional.of(salesOrder));

        sqsReceiveService.queueListenerEcpShopOrders(rawMessage, senderId, ANY_RECEIVE_COUNT);

        verify(camundaHelper, never()).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(salesOrderService).getOrderByOrderNumber(eq("524001240"));
    }

    @Test
    void testQueueListenerInvoiceCreatedMsgReceived() {
        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.createSalesOrderForInvoice(any(), any(), any())).thenReturn(salesOrder);
        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.createOrderNumberInSOH(any(), any())).thenReturn(salesOrder.getOrderNumber(), "10");

        String coreSalesInvoiceCreatedMessage = readResource("examples/coreSalesInvoiceCreatedOneItem.json");
        sqsReceiveService.queueListenerCoreSalesInvoiceCreated(coreSalesInvoiceCreatedMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT);

        verify(salesOrderService).createSalesOrderForInvoice(any(CoreSalesInvoiceCreatedMessage.class), any(SalesOrder.class),any(String.class));
        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedWithPaypalPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, PAYPAL, NEW);
        salesOrder.setOrderNumber("500000996");

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(anyString());
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);
        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured("500000996");

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedWithCreditcardPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber("500000996");

        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(true);

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedSalesOrderNotFoundException() {
        String rawMessage =  readResource("examples/coreDataReaderEvent.json");

        var exception = new SalesOrderNotFoundException("500000996");

        when(salesOrderService.getOrderByOrderNumber(any())).thenThrow(exception);

        assertThatThrownBy(() -> sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(SalesOrderNotFoundException.class)
                .hasMessage(exception.getMessage());

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedIllegalArgumentException() {

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedRuntimeExceptionException() {

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(false);
        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        assertThatThrownBy(() -> sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerD365OrderPaymentSecuredReceived() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        sqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567787")),
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567858"))
        );

        verifyNoMoreInteractions(salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerD365OrderPaymentSecuredReceivedThrownException() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");

        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(
                "4567787", "4567858");

        assertThatThrownBy(() -> sqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT)).isExactlyInstanceOf(RuntimeException.class);

    }

    @Test
    @SneakyThrows
    void testQueueListenerDropshipmentPurchaseOrderBooked() {

        String rawMessage = readResource("examples/dropshipmentOrderPurchasedBooked.json");

        sqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderBookedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderBookedMessage.class);
        verify(salesOrderRowService).handleDropshipmentPurchaseOrderBooked(message);
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreatedDuplication() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        when(salesOrderService.getOrderByOrderNumber(eq(salesOrder.getOrderNumber()))).thenReturn(Optional.of(salesOrder));

        sqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationOrderCreated(salesOrder.getOrderNumber());
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreatedNewOrder() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");
        SalesOrder salesOrder = getSalesOrder(rawMessage);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.empty());
        when(salesOrderService.createSalesOrder(any())).thenReturn(salesOrder);

        sqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(camundaHelper).createOrderProcess(eq(salesOrder), any(Messages.class));
    }

    @Test
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedDuplication() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");
        var creditNoteMsg = getCreditNoteMsg(rawEventMessage);
        var orderNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        SalesOrder salesOrder = createSalesOrder(orderNumber);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, creditNoteNumber);
        when(salesOrderReturnService.getByOrderNumber(eq(salesOrderReturn.getOrderNumber()))).thenReturn(salesOrderReturn);
        when(salesOrderService.createOrderNumberInSOH(eq(orderNumber), eq(creditNoteNumber))).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));

        sqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(salesOrderReturn);
    }

    @Test
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedNewCreditNote() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");
        var creditNoteMsg = getCreditNoteMsg(rawEventMessage);
        var orderNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        when(salesOrderReturnService.getByOrderNumber(any())).thenReturn(null);
        when(salesOrderService.createOrderNumberInSOH(eq(orderNumber), eq(creditNoteNumber))).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));

        sqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderRowService).handleSalesOrderReturn(eq(orderNumber), eq(creditNoteMsg));
    }

    private SalesOrder createSalesOrder(String orderNumber) {
        String rawOrderMessage = readResource("examples/ecpOrderMessage.json");
        Order order = getOrder(rawOrderMessage);
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderNumber);
        return createSalesOrderFromOrder(order);
    }

    @SneakyThrows({URISyntaxException.class, IOException.class})
    private String readResource(String path) {
        return java.nio.file.Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(path))
                        .toURI()));
    }
}
