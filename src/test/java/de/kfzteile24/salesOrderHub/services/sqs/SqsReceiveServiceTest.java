package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.FileUtil;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapperImpl;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.SplitterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.PAYPAL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createSalesOrderFromOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getCreditNoteMsg;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrderReturn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private SalesOrderProcessService salesOrderProcessService;
    @Mock
    private SleuthHelper sleuthHelper;
    @Mock
    private MessageWrapperUtil messageWrapperUtil;
    @Mock
    private SplitterService splitterService;
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
    private OrderUtil orderUtil;
    @Mock
    private DropshipmentOrderService dropshipmentOrderService;
    @Spy
    private MessageErrorHandler messageErrorHandler;
    @Mock
    private CreditNoteEventMapper creditNoteEventMapper;
    @InjectMocks
    @Spy
    private SqsReceiveService sqsReceiveService;
    @Mock
    private ParcelShippedService parcelShippedService;
    @Mock
    private CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;
    @Mock
    private SQSNamesConfig sqsNamesConfig;
    @Spy
    private final SalesOrderMapper salesOrderMapper = new SalesOrderMapperImpl();

    @BeforeEach
    void setUp() {
        sqsReceiveService.setObjectMapper(objectMapper);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedWithPaypalPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, PAYPAL, NEW);
        salesOrder.setOrderNumber("500000996");

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(anyString());
        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);
        sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);
        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured("500000996");

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerOrderPaymentSecuredReceivedWithCreditcardPayment() {

        SalesOrder salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber("500000996");

        when(salesOrderService.getOrderByOrderNumber(anyString())).thenReturn(Optional.of(salesOrder));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(true);

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);
        sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).getOrderByOrderNumber("500000996");
        verify(salesOrderPaymentSecuredService).hasOrderPaypalPaymentType(salesOrder);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerOrderPaymentSecuredReceivedSalesOrderNotFoundException() {
        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);

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
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenThrow(IllegalArgumentException.class);

        assertThatThrownBy(() -> sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerOrderPaymentSecuredReceivedRuntimeExceptionException() {

        String rawMessage =  readResource("examples/coreDataReaderEvent.json");
        mockMessageWrapper(rawMessage,  CoreDataReaderEvent.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(new SalesOrder()));
        when(salesOrderPaymentSecuredService.hasOrderPaypalPaymentType(any())).thenReturn(false);
        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        assertThatThrownBy(() -> sqsReceiveService.queueListenerOrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT))
                .isExactlyInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(salesOrderService, salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceived() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");
        mockMessageWrapper(rawMessage,  OrderPaymentSecuredMessage.class);

        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        sqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567787")),
                argThat(orderNumber -> StringUtils.equals(orderNumber, "4567858"))
        );

        verifyNoMoreInteractions(salesOrderPaymentSecuredService);
    }

    @Test
    void testQueueListenerD365OrderPaymentSecuredDropshipmentOrderReceived() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");
        mockMessageWrapper(rawMessage,  OrderPaymentSecuredMessage.class);


        when(dropshipmentOrderService.isDropShipmentOrder("4567787")).thenReturn(true);
        doNothing().when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(any());

        sqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured( "4567858");

        verifyNoMoreInteractions(salesOrderPaymentSecuredService);
    }

    @Test
    @SneakyThrows
    void testQueueListenerD365OrderPaymentSecuredReceivedThrownException() {
        String rawMessage =  readResource("examples/d365OrderPaymentSecuredMessageWithTwoOrderNumbers.json");
        mockMessageWrapper(rawMessage,  OrderPaymentSecuredMessage.class);

        doThrow(RuntimeException.class).when(salesOrderPaymentSecuredService).correlateOrderReceivedPaymentSecured(
                "4567787", "4567858");

        assertThatThrownBy(() -> sqsReceiveService.queueListenerD365OrderPaymentSecured(rawMessage, ANY_SENDER_ID,
                ANY_RECEIVE_COUNT)).isExactlyInstanceOf(RuntimeException.class);

    }

    @Test
    @SneakyThrows
    void testQueueListenerDropshipmentPurchaseOrderBooked() {

        String rawMessage = readResource("examples/dropshipmentOrderPurchasedBooked.json");
        mockMessageWrapper(rawMessage,  DropshipmentPurchaseOrderBookedMessage.class);

        sqsReceiveService.queueListenerDropshipmentPurchaseOrderBooked(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderBookedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderBookedMessage.class);
        verify(dropshipmentOrderService).handleDropShipmentOrderConfirmed(message);
    }

    @Test
    void testQueueListenerMigrationCoreSalesOrderCreatedDuplication() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");
        mockMessageWrapper(rawMessage,  Order.class);
        SalesOrder salesOrder = getSalesOrder(rawMessage);
        when(salesOrderService.getOrderByOrderNumber(eq(salesOrder.getOrderNumber()))).thenReturn(Optional.of(salesOrder));
        when(featureFlagConfig.getIgnoreMigrationCoreSalesOrder()).thenReturn(false);

        sqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).enrichSalesOrder(salesOrder, salesOrder.getLatestJson(), (Order) salesOrder.getOriginalOrder());
        verify(salesOrderService).save(argThat(so -> {
                    assertThat(so).isEqualTo(salesOrder);
                    return true;
                }
        ), eq(MIGRATION_SALES_ORDER_RECEIVED));
        verify(snsPublishService).publishMigrationOrderCreated(salesOrder.getOrderNumber());
    }

    @Test
    @SneakyThrows
    void testQueueListenerMigrationCoreSalesOrderCreatedNewOrder() {

        String rawMessage = readResource("examples/ecpOrderMessage.json");
        mockMessageWrapper(rawMessage,  Order.class);
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        var order = objectMapper.readValue(sqsMessage.getBody(), Order.class);

        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.empty());
        when(featureFlagConfig.getIgnoreMigrationCoreSalesOrder()).thenReturn(false);

        sqsReceiveService.queueListenerMigrationCoreSalesOrderCreated(rawMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(salesOrderService).createSalesOrder(argThat(salesOrder -> {
            assertThat(salesOrder.getOrderNumber()).isEqualTo(order.getOrderHeader().getOrderNumber());
            assertThat(salesOrder.getLatestJson()).isEqualTo(order);
            assertThat(salesOrder.getVersion()).isEqualTo(3L);
            assertThat(salesOrder.getOriginalOrder()).isEqualTo(order);
            assertThat(salesOrder.getCustomerEmail()).isEqualTo(order.getOrderHeader().getCustomer().getCustomerEmail());
            assertThat(salesOrder.getOrderGroupId()).isEqualTo(order.getOrderHeader().getOrderGroupId());
            assertThat(salesOrder.getSalesChannel()).isEqualTo(order.getOrderHeader().getSalesChannel());
            return true;
            }
        ));
        verify(snsPublishService).publishMigrationOrderCreated(order.getOrderHeader().getOrderNumber());
    }

    @Test
    void testQueueListenerCoreSalesCreditNoteCreated() {

        String invoiceMsg = readResource("examples/coreSalesCreditNoteCreated.json");
        String sqsName = sqsNamesConfig.getCoreSalesCreditNoteCreated();

        sqsReceiveService.queueListenerCoreSalesCreditNoteCreated(invoiceMsg, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(coreSalesCreditNoteCreatedService).handleCoreSalesCreditNoteCreated(invoiceMsg, ANY_RECEIVE_COUNT, sqsName);

    }
    @Test
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedDuplication() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");
        mockMessageWrapper(rawEventMessage,  SalesCreditNoteCreatedMessage.class);
        var creditNoteMsg = getCreditNoteMsg(rawEventMessage);
        var orderNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();

        SalesOrder salesOrder = createSalesOrder(orderNumber);
        SalesOrderReturn salesOrderReturn = getSalesOrderReturn(salesOrder, creditNoteNumber);
        when(salesOrderReturnService.getByOrderNumber(eq(salesOrderReturn.getOrderNumber()))).thenReturn(salesOrderReturn);
        when(salesOrderService.createOrderNumberInSOH(eq(orderNumber), eq(creditNoteNumber))).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));
        when(featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote()).thenReturn(false);

        sqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(snsPublishService).publishMigrationReturnOrderCreatedEvent(salesOrderReturn);
    }

    @Test
    void testQueueListenerMigrationCoreSalesCreditNoteCreatedNewCreditNote() {

        String rawEventMessage = readResource("examples/coreSalesCreditNoteCreated.json");
        mockMessageWrapper(rawEventMessage,  SalesCreditNoteCreatedMessage.class);
        var creditNoteMsg = getCreditNoteMsg(rawEventMessage);
        var orderNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        var creditNoteNumber = creditNoteMsg.getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber();
        String sqsName = sqsNamesConfig.getCoreSalesCreditNoteCreated();

        when(salesOrderReturnService.getByOrderNumber(any())).thenReturn(null);
        when(salesOrderService.createOrderNumberInSOH(eq(orderNumber), eq(creditNoteNumber))).thenReturn(createOrderNumberInSOH(orderNumber, creditNoteNumber));
        when(featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote()).thenReturn(false);

        sqsReceiveService.queueListenerMigrationCoreSalesCreditNoteCreated(rawEventMessage, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(coreSalesCreditNoteCreatedService).handleCoreSalesCreditNoteCreated(rawEventMessage,ANY_RECEIVE_COUNT, sqsName);
    }

    @Test
    void testQueueListenerParcelShipped() {

        String invoiceMsg = readResource("examples/parcelShipped.json");
        String sqsName = sqsNamesConfig.getParcelShipped();

        sqsReceiveService.queueListenerParcelShipped(invoiceMsg, ANY_SENDER_ID, ANY_RECEIVE_COUNT);

        verify(parcelShippedService).handleParcelShipped(invoiceMsg, ANY_RECEIVE_COUNT, sqsName);

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
        return FileUtil.readResource(getClass(), path);
    }

    @SneakyThrows
    private <T> void mockMessageWrapper(String rawMessage, Class<T> clazz) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        T message = objectMapper.readValue(body, clazz);
        var messageWrapper = MessageWrapper.<T>builder()
                .message(message)
                .rawMessage(rawMessage)
                .build();
        when(messageWrapperUtil.create(eq(rawMessage), eq(clazz)))
                .thenReturn(messageWrapper);
    }


}
