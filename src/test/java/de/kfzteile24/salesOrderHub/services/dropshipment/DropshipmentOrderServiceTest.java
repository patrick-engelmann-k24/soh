package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.constants.PersistentProperties;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.InvoiceData;
import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNote;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.ReturnOrderHelper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.helper.SubsequentSalesOrderCreationHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.salesOrderHub.services.property.KeyValuePropertyService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderHeader;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.K24;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.DROPSHIPMENT_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DropshipmentOrderServiceTest {

    public static final String ANY_INVOICE_NUMBER = RandomStringUtils.randomNumeric(10);

    @InjectMocks
    @Spy
    private DropshipmentOrderService dropshipmentOrderService;
    @Mock
    private KeyValuePropertyService keyValuePropertyService;
    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private SalesOrderReturnService salesOrderReturnService;
    @Mock
    private SnsPublishService snsPublishService;
    @Mock
    private InvoiceService invoiceService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CamundaHelper camundaHelper;
    @Mock
    private ReturnOrderHelper returnOrderHelper;
    @Spy
    private ObjectMapper objectMapper;
    @Mock
    private OrderUtil orderUtil;
    @Mock
    private MetricsHelper metricsHelper;
    @Mock
    private SubsequentSalesOrderCreationHelper subsequentSalesOrderCreationHelper;
    @Mock
    private SalesOrderRowService salesOrderRowService;
    private final MessageWrapper messageWrapper = MessageWrapper.builder().build();

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
    void testHandleDropshipmentPurchaseOrderReturnConfirmed() {
        var message = getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        var salesCreditNoteCreatedMessage = SalesCreditNoteCreatedMessage.builder()
                .salesCreditNote(SalesCreditNote.builder()
                        .salesCreditNoteHeader(SalesCreditNoteHeader.builder()
                                .creditNoteNumber("2022200002")
                                .orderNumber(message.getSalesOrderNumber())
                                .build())
                        .build())
                .build();

        when(keyValuePropertyService.getPropertyByKey(PersistentProperties.PREVENT_DROPSHIPMENT_ORDER_RETURN_CONFIRMED))
                .thenReturn(Optional.of(KeyValueProperty.builder().typedValue(false).build()));
        doReturn(salesCreditNoteCreatedMessage).when(dropshipmentOrderService).buildSalesCreditNoteCreatedMessage(message);

        when(salesOrderService.getOrderByOrderNumber(message.getSalesOrderNumber())).thenReturn(Optional.of(SalesOrder.builder().build()));
        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message, messageWrapper);
        verify(salesOrderReturnService).handleSalesOrderReturn(salesCreditNoteCreatedMessage, DROPSHIPMENT_PURCHASE_ORDER_RETURN_CONFIRMED, DROPSHIPMENT_ORDER_RETURN_CONFIRMED);
    }

    @Test
    @SneakyThrows
    void testHandleDropshipmentPurchaseOrderReturnConfirmedPrevented() {
        var message = getObjectByResource("dropshipmentPurchaseOrderReturnConfirmed.json", DropshipmentPurchaseOrderReturnConfirmedMessage.class);
        var expectedErrorMessage = MessageFormat.format(
                "Dropshipment Order Return Confirmed process is inactive. " +
                        "Message with Order number {0} is moved to DLQ", message.getSalesOrderNumber());
        when(keyValuePropertyService.getPropertyByKey(PersistentProperties.PREVENT_DROPSHIPMENT_ORDER_RETURN_CONFIRMED))
                .thenReturn(Optional.of(KeyValueProperty.builder().typedValue(true).build()));

        assertThatThrownBy(() -> dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(
                message, messageWrapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedErrorMessage);
    }

    @Test
    @SneakyThrows
    void testHandleDropshipmentPurchaseOrderReturnNotified() {

        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));

        var message = getObjectByResource("dropshipmentPurchaseOrderReturnNotified.json", DropshipmentPurchaseOrderReturnNotifiedMessage.class);

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnNotified(message, messageWrapper);

        verify(snsPublishService).publishDropshipmentOrderReturnNotifiedEvent(salesOrder, message);
    }

    @Test
    void testRecreateSalesOrderInvoice() {
        var salesOrder = getSalesOrder(getObjectByResource("testmessage.json", Order.class));
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

    @ParameterizedTest
    @ValueSource(strings = {
            "dropshipmentOrderPurchasedBooked.json",
            "dropshipmentOrderPurchasedBookedFalse.json"
    })
    void testHandleDropShipmentOrderConfirmedBookedTrue(String filename) {

        var message = getObjectByResource(filename, DropshipmentPurchaseOrderBookedMessage.class);
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);

        when(salesOrderService.getOrderByOrderNumber(message.getSalesOrderNumber())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.save(salesOrder, DROPSHIPMENT_PURCHASE_ORDER_BOOKED)).thenReturn(salesOrder);
        doNothing().when(camundaHelper).correlateMessage(eq(DROPSHIPMENT_ORDER_CONFIRMED), eq(salesOrder), any());

        dropshipmentOrderService.handleDropShipmentOrderConfirmed(message, messageWrapper);

        verify(salesOrderService).save(
                argThat(so -> so.getLatestJson().getOrderHeader().getOrderNumberExternal()
                        .equalsIgnoreCase(message.getExternalOrderNumber())),
                eq(DROPSHIPMENT_PURCHASE_ORDER_BOOKED));
        verify(camundaHelper).correlateMessage(eq(DROPSHIPMENT_ORDER_CONFIRMED), eq(salesOrder),
                eq(Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), message.getBooked())));
    }

    private static Stream<Arguments> provideArgumentsForIsDropShipmentOrder() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(StringUtils.EMPTY, false),
                Arguments.of(K24.getName(), false),
                Arguments.of(DELTICOM.getName(), true)
        );
    }

    @Test
    @SneakyThrows
    void testCreateDropshipmentNewOrderNumber() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        when(salesOrderService.getNextOrderNumberIndexCounter(salesOrder.getOrderGroupId())).thenReturn(1);
        String newOrderNumber = dropshipmentOrderService.createDropshipmentNewOrderNumber(salesOrder);
        assertThat(newOrderNumber).isEqualTo(salesOrder.getOrderGroupId() + "-1");
    }

    @Test
    @SneakyThrows
    void testCreateDropshipmentSubsequentSalesOrder() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        var skuList = List.of("sku-1", "sku-2");
        val differentQuantity= salesOrder.getLatestJson().getOrderRows().get(0).getQuantity().add(BigDecimal.valueOf(1));
        val sameQuantity= salesOrder.getLatestJson().getOrderRows().get(1).getQuantity();
        var quantityList = List.of(differentQuantity.intValue(), sameQuantity.intValue());
        var invoiceNumber = "2023";
        var newOrderNumber = salesOrder.getOrderGroupId()+"-100";
        var activityInstanceId = "1010";
        val invoiceData = new InvoiceData(invoiceNumber, newOrderNumber, skuList, quantityList);
        var orderHeader = buildExpectedOrderHeader(salesOrder, newOrderNumber, invoiceNumber);
        when(salesOrderService.save(any(), any())).thenAnswer((Answer<SalesOrder>) invocation -> invocation.getArgument(0));
        when(salesOrderService.getNextOrderNumberIndexCounter(eq(salesOrder.getOrderGroupId()))).thenReturn(100);
        when(subsequentSalesOrderCreationHelper.createOrderHeader(any(), anyString(), anyString())).thenReturn(orderHeader);
        when(invoiceService.getShippingCostLine(any())).thenReturn(null);
        doNothing().when(salesOrderService).recalculateTotals(any(), any());

        var subsequentOrder = dropshipmentOrderService.createDropshipmentSubsequentSalesOrder(salesOrder, invoiceData.getSkuQuantityMap(), invoiceNumber, activityInstanceId);

        verify(salesOrderService).recalculateSumValues(eq(salesOrder.getLatestJson().getOrderRows().get(0)), eq(differentQuantity));
        verify(salesOrderService, never()).recalculateSumValues(eq(salesOrder.getLatestJson().getOrderRows().get(1)), eq(sameQuantity));
        assertThat(subsequentOrder.getOrderNumber()).isEqualTo(newOrderNumber);
        assertThat(subsequentOrder.getOrderGroupId()).isEqualTo(salesOrder.getOrderNumber());
        assertThat(subsequentOrder.getSalesChannel()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getSalesChannel());
        assertThat(subsequentOrder.getCustomerEmail()).isEqualTo(salesOrder.getLatestJson().getOrderHeader().getCustomer().getCustomerEmail());
        assertThat(subsequentOrder.getLatestJson().getOrderRows().stream()
                .map(OrderRows::getSku)
                .collect(Collectors.toList()))
                .containsExactly("sku-1", "sku-2");
        assertThat(subsequentOrder.getProcessId()).isEqualTo(activityInstanceId);

    }

    private OrderHeader buildExpectedOrderHeader(SalesOrder salesOrder, String newOrderNumber, String invoiceNumber) throws JsonProcessingException {
        Order order = copyOrder(salesOrder);
        order.getOrderHeader().setOrderNumber(newOrderNumber);
        order.getOrderHeader().setOrderGroupId(salesOrder.getOrderNumber());
        order.getOrderHeader().setDocumentRefNumber(invoiceNumber);
        return order.getOrderHeader();
    }

    @Test
    @SneakyThrows
    void testCreateDropshipmentSubsequentOrderJson() {

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(
                false, REGULAR, CREDIT_CARD, NEW);
        var newOrderNumber = salesOrder.getOrderNumber() + "-1";
        var skuList = List.of("sku-3", "sku-2");
        var quantities = List.of(0, 0);
        var invoiceNumber = "123";
        val invoiceData = new InvoiceData(invoiceNumber, newOrderNumber, skuList, quantities);

        when(invoiceService.getShippingCostLine(eq(salesOrder))).thenReturn(null);
        doNothing().when(salesOrderService).recalculateTotals(any(), any());
        doReturn(salesOrder.getLatestJson().getOrderHeader()).when(subsequentSalesOrderCreationHelper).createOrderHeader(any(), anyString(), anyString());

        Order result = dropshipmentOrderService.createDropshipmentSubsequentOrderJson(salesOrder, newOrderNumber,
                invoiceData.getSkuQuantityMap(), invoiceNumber);

        assertThat(result.getOrderRows().stream().map(OrderRows::getSku).collect(Collectors.toList())).containsOnly("sku-3", "sku-2");
        assertEquals(BigDecimal.ZERO, salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostGross());
        assertEquals(BigDecimal.ZERO, salesOrder.getLatestJson().getOrderHeader().getTotals().getShippingCostNet());
    }

    private Order copyOrder(SalesOrder salesOrder) throws JsonProcessingException {
        var salesOrderJsonString = objectMapper.writeValueAsString(salesOrder.getLatestJson());
        return objectMapper.readValue(salesOrderJsonString, Order.class);
    }
}