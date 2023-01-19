package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.BpmItem;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoice;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.bpm.extension.mockito.process.ProcessInstanceFake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.getSalesOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreSalesInvoiceServiceTest {

    public static final String ANY_PROCESS_ID = UUID.randomUUID().toString();
    public static final UUID ANY_SALES_ORDER_ID = UUID.randomUUID();

    @Mock
    private SalesOrderService salesOrderService;
    @Mock
    private SalesOrderRowService salesOrderRowService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CamundaHelper camundaHelper;
    @Mock
    private FeatureFlagConfig featureFlagConfig;
    @Mock
    private SnsPublishService snsPublishService;
    @Mock
    private OrderUtil orderUtil;
    @Mock
    private MetricsHelper metricsHelper;
    @InjectMocks
    @Spy
    private CoreSalesInvoiceService coreSalesInvoiceCreatedService;

    @Test
    @DisplayName("Test That Subsequent Order should be created, even if Invoice is Fully Matched With Original Order, but sales invoice event was already published for the Original Order")
    void testQueueListenerSubsequentOrderCreatedIfSalesInvoicePublished() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        salesOrder.setProcessId(ANY_PROCESS_ID);
        salesOrder.setId(ANY_SALES_ORDER_ID);
        salesOrder.setOrderGroupId(salesOrder.getOrderNumber());
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber("not null");
        salesOrder.setInvoiceEvent(new CoreSalesInvoiceCreatedMessage(new CoreSalesInvoice(new CoreSalesInvoiceHeader(), Collections.emptyList())));

        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.checkOrderNotExists(any())).thenReturn(true);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.createSalesOrderForInvoice(any(), any(), any())).thenReturn(salesOrder);
        when(salesOrderService.createOrderNumberInSOH(any(), any())).thenReturn(salesOrder.getOrderNumber(), "10");
        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);
        when(camundaHelper.waitsOnActivityForMessage(anyString(), any(BpmItem.class), any(BpmItem.class))).thenReturn(false);

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        coreSalesInvoiceCreatedService.setPublishDelayForSubsequentOrders(StringUtils.EMPTY);
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);

        verify(camundaHelper).waitsOnActivityForMessage(salesOrder.getProcessId(), MSG_ORDER_CORE_SALES_INVOICE_CREATED,
                ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED);
        verify(camundaHelper, never()).correlateMessage(eq(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED), anyString());
        verify(salesOrderService).createSalesOrderForInvoice(any(CoreSalesInvoiceCreatedMessage.class), any(SalesOrder.class), any(String.class));
        verify(coreSalesInvoiceCreatedService).startInvoiceCreatedReceivedProcess(salesOrder);
    }

    @Test
    void testQueueListenerInvoiceCreatedMsgReceived() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        salesOrder.setProcessId(ANY_PROCESS_ID);
        salesOrder.setId(ANY_SALES_ORDER_ID);
        salesOrder.setOrderGroupId(salesOrder.getOrderNumber());

        when(salesOrderService.checkOrderNotExists(any())).thenReturn(true);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.createSalesOrderForInvoice(any(), any(), any())).thenReturn(salesOrder);
        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.createOrderNumberInSOH(any(), any())).thenReturn(salesOrder.getOrderNumber(), "10");
        when(orderUtil.checkIfOrderHasOrderRows(any())).thenReturn(true);
        when(camundaHelper.waitsOnActivityForMessage(anyString(), any(BpmItem.class), any(BpmItem.class))).thenReturn(true);
        doNothing().when(camundaHelper).correlateMessage(any(), eq(salesOrder), any());

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        coreSalesInvoiceCreatedService.setPublishDelayForSubsequentOrders(StringUtils.EMPTY);
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);

        verify(camundaHelper).waitsOnActivityForMessage(salesOrder.getProcessId(), MSG_ORDER_CORE_SALES_INVOICE_CREATED,
                ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED);
        verify(camundaHelper).correlateMessage(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED, salesOrder, Variables.putValue(IS_ORDER_CANCELLED.getName(), true));
        verify(salesOrderService).createSalesOrderForInvoice(any(CoreSalesInvoiceCreatedMessage.class), any(SalesOrder.class),any(String.class));
        verify(coreSalesInvoiceCreatedService).startInvoiceCreatedReceivedProcess(salesOrder);
    }

    @Test
    @DisplayName("Test That Original Order should be updated, if Invoice is Fully Matched With Original Order and sales invoice event was not published for the Original Order")
    void testQueueListenerOriginalOrderUpdateIfSalesInvoiceNotPublished() {
        SalesOrder salesOrder = getSalesOrder(getObjectByResource("ecpOrderMessage.json", Order.class));
        String orderNumber = salesOrder.getOrderNumber();
        salesOrder.setProcessId(ANY_PROCESS_ID);
        salesOrder.setOrderGroupId(orderNumber);
        salesOrder.setId(ANY_SALES_ORDER_ID);
        salesOrder.getLatestJson().getOrderHeader().setOrderGroupId(orderNumber);
        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        assertNotNull(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertNull(salesOrder.getInvoiceEvent());

        when(featureFlagConfig.getIgnoreCoreSalesInvoice()).thenReturn(false);
        when(salesOrderService.getOrderByOrderNumber(any())).thenReturn(Optional.of(salesOrder));
        when(salesOrderService.isFullyMatchedWithOriginalOrder(eq(salesOrder), any())).thenReturn(true);
        when(camundaHelper.waitsOnActivityForMessage(anyString(), any(BpmItem.class), any(BpmItem.class))).thenReturn(false);

        coreSalesInvoiceCreatedService.setPublishDelayForSubsequentOrders(StringUtils.EMPTY);
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(message, messageWrapper);

        verify(camundaHelper).waitsOnActivityForMessage(salesOrder.getProcessId(), MSG_ORDER_CORE_SALES_INVOICE_CREATED,
                ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED);
        verify(camundaHelper, never()).correlateMessage(eq(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED), anyString());
        verify(salesOrderService).updateOrder(salesOrder);
        verify(coreSalesInvoiceCreatedService).startInvoiceCreatedReceivedProcess(salesOrder);

        assertNotNull(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertEquals(salesOrder.getLatestJson().getOrderHeader().getOrderGroupId(), salesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderGroupId());
    }
}
