package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.SplitterService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.soh.order.dto.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_CREATED_IN_SOH;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreSalesInvoiceCreatedServiceTest {

    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private SalesOrderProcessService salesOrderProcessService;
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
    @Mock
    private CreditNoteEventMapper creditNoteEventMapper;
    @InjectMocks
    @Spy
    private CoreSalesInvoiceCreatedService coreSalesInvoiceCreatedService;

    @Test
    @DisplayName("Test That Subsequent Order should be created, even if Invoice is Fully Matched With Original Order, but sales invoice event was already published for the Original Order")
    void testQueueListenerSubsequentOrderCreatedIfSalesInvoicePublished() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber("not null");
        salesOrder.setInvoiceEvent(new CoreSalesInvoiceCreatedMessage(new CoreSalesInvoice(new CoreSalesInvoiceHeader(), Collections.emptyList())));

        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.checkOrderNotExists(any())).thenReturn(true);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.createSalesOrderForInvoice(any(), any(), any())).thenReturn(salesOrder);
        when(salesOrderService.createOrderNumberInSOH(any(), any())).thenReturn(salesOrder.getOrderNumber(), "10");
        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);

        verify(salesOrderService).createSalesOrderForInvoice(any(CoreSalesInvoiceCreatedMessage.class), any(SalesOrder.class), any(String.class));
        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), eq(ORDER_CREATED_IN_SOH));
        verify(camundaHelper).startInvoiceCreatedReceivedProcess(salesOrder);
    }

    @Test
    void testQueueListenerInvoiceCreatedMsgReceived() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));

        when(salesOrderService.checkOrderNotExists(any())).thenReturn(true);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.createSalesOrderForInvoice(any(), any(), any())).thenReturn(salesOrder);
        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.createOrderNumberInSOH(any(), any())).thenReturn(salesOrder.getOrderNumber(), "10");
        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);

        verify(salesOrderService).createSalesOrderForInvoice(any(CoreSalesInvoiceCreatedMessage.class), any(SalesOrder.class),any(String.class));
        verify(camundaHelper).createOrderProcess(any(SalesOrder.class), any(Messages.class));
        verify(camundaHelper).startInvoiceCreatedReceivedProcess(salesOrder);
    }

    @Test
    @DisplayName("Test That Original Order should be updated, if Invoice is Fully Matched With Original Order and sales invoice event was not published for the Original Order")
    void testQueueListenerOriginalOrderUpdateIfSalesInvoiceNotPublished() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        String orderNumber = salesOrder.getOrderNumber();
        salesOrder.setOrderGroupId(orderNumber);
        salesOrder.getLatestJson().getOrderHeader().setOrderGroupId(orderNumber);
        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        assertNotNull(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertNull(salesOrder.getInvoiceEvent());

        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.isFullyMatchedWithOriginalOrder(eq(salesOrder), any())).thenReturn(true);

        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);

        verify(salesOrderService).updateOrder(salesOrder);
        verify(camundaHelper).startInvoiceCreatedReceivedProcess(salesOrder);

        assertNotNull(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertEquals(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId(), salesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderGroupId());
    }
}
