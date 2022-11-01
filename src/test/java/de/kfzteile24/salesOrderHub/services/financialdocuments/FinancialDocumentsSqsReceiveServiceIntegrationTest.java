package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.shared.creditnote.SalesCreditNoteHeader;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.ObjectUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.GrandTotalTaxes;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.SumValues;
import de.kfzteile24.soh.order.dto.Totals;
import de.kfzteile24.soh.order.dto.UnitValues;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.ORDER_NUMBER_SEPARATOR;
import static de.kfzteile24.salesOrderHub.constants.SOHConstants.RETURN_ORDER_NUMBER_PREFIX;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_PAYMENT_SECURED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_GROUP_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.helper.JsonTestUtil.getObjectByResource;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createOrderNumberInSOH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@Slf4j
class FinancialDocumentsSqsReceiveServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvoiceNumberCounterRepository invoiceNumberCounterRepository;
    @Autowired
    private InvoiceNumberCounterService invoiceNumberCounterService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private SalesOrderUtil salesOrderUtil;
    @Autowired
    private ObjectUtil objectUtil;

    @BeforeEach
    public void setup() {
        super.setUp();
        bpmUtil.cleanUp();
        salesOrderRepository.deleteAll();
        auditLogRepository.deleteAll();
        invoiceNumberCounterRepository.deleteAll();
        invoiceNumberCounterService.init();
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> bpmUtil.cleanUp());
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
    }

    @SneakyThrows
    @Test
    @DisplayName("IT core sales credit note created event handling")
    void testQueueListenerCoreSalesCreditNoteCreated(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderGroupId("580309129");
        salesOrder.setOrderNumber("580309129");
        salesOrder.getLatestJson().getOrderHeader().getTotals().setGrandTotalTaxes(List.of(GrandTotalTaxes.builder()
                .rate(BigDecimal.valueOf(19))
                .value(BigDecimal.valueOf(0.38))
                .build()));

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();
        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(message, messageWrapper);

        SalesCreditNoteHeader salesCreditNoteHeader = message.getSalesCreditNote().getSalesCreditNoteHeader();
        String returnOrderNumber = RETURN_ORDER_NUMBER_PREFIX + ORDER_NUMBER_SEPARATOR + salesCreditNoteHeader.getCreditNoteNumber();
        SalesOrderReturn returnSalesOrder = salesOrderReturnService.getByOrderNumber(returnOrderNumber)
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(returnOrderNumber));
        Order returnOrder = returnSalesOrder.getReturnOrderJson();

        checkOrderRowValues(returnOrder.getOrderRows());
        checkTotalsValues(returnOrder.getOrderHeader().getTotals());
        checkEventIsPublished(message);
    }

    @SneakyThrows
    @Test
    @DisplayName("IT core sales credit note created event handling with dropshipment order sample")
    void testQueueListenerCoreSalesCreditNoteCreatedWithDropshipmentOrderSample(TestInfo testInfo) {

        log.info(testInfo.getDisplayName());

        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderGroupId("580309129");
        salesOrder.setOrderNumber("580309129-1");
        salesOrder.getLatestJson().getOrderHeader().getTotals().setGrandTotalTaxes(List.of(GrandTotalTaxes.builder()
                .rate(BigDecimal.valueOf(19))
                .value(BigDecimal.valueOf(0.38))
                .build()));

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);

        var message = getObjectByResource("coreSalesCreditNoteCreated.json", SalesCreditNoteCreatedMessage.class);
        message.getSalesCreditNote().getSalesCreditNoteHeader().setOrderNumber("580309129-1");
        var messageWrapper = MessageWrapper.builder().build();
        financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(message, messageWrapper);

        verify(salesOrderService).getOrderByOrderNumber("580309129-1");
        verify(salesOrderReturnService).handleSalesOrderReturn(message, RETURN_ORDER_CREATED, CORE_CREDIT_NOTE_CREATED);
        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo("RO-876130");
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo("580309129");
                    return true;
                }
        ));
    }

    private void checkOrderRowValues(List<OrderRows> returnOrderRows) {

        assertEquals(3, returnOrderRows.size());

        OrderRows orderRows = returnOrderRows.stream().filter(r -> r.getSku().equals("sku-1")).findFirst().orElse(null);
        checkOrderRowValues(orderRows, "name-sku-1",
                new BigDecimal("1"), new BigDecimal("19.00"),
                new BigDecimal("-1.00"), new BigDecimal("-1.19"),
                new BigDecimal("-1.00"), new BigDecimal("-1.19"));

        orderRows = returnOrderRows.stream().filter(r -> r.getSku().equals("new-sku")).findFirst().orElse(null);
        checkOrderRowValues(orderRows, "new-sku",
                new BigDecimal("1"), new BigDecimal("19.00"),
                new BigDecimal("10.00"), new BigDecimal("11.90"),
                new BigDecimal("10.00"), new BigDecimal("11.90"));

        orderRows = returnOrderRows.stream().filter(r -> r.getSku().equals("sku-2")).findFirst().orElse(null);
        checkOrderRowValues(orderRows, "name-sku-2",
                new BigDecimal("-2"), new BigDecimal("20.00"),
                new BigDecimal("17.39"), new BigDecimal("20.87"),
                new BigDecimal("-34.78"), new BigDecimal("-41.74"));
    }

    private void checkOrderRowValues(OrderRows orderRows, String itemDescription,
                                     BigDecimal quantity, BigDecimal taxRate,
                                     BigDecimal unitNetValue, BigDecimal unitGrossValue,
                                     BigDecimal sumNetValue, BigDecimal sumGrossValue) {

        assertNotNull(orderRows);
        assertEquals(itemDescription, orderRows.getName());
        assertEquals(quantity, orderRows.getQuantity());
        assertEquals(taxRate, orderRows.getTaxRate());
        assertEquals("shipment_regular", orderRows.getShippingType());

        assertEquals(unitNetValue, orderRows.getUnitValues().getGoodsValueNet());
        assertEquals(unitGrossValue, orderRows.getUnitValues().getGoodsValueGross());

        assertEquals(sumNetValue, orderRows.getSumValues().getGoodsValueNet());
        assertEquals(sumGrossValue, orderRows.getSumValues().getGoodsValueGross());
    }

    private void checkTotalsValues(Totals totals) {

        assertNotNull(totals);

        assertEquals(new BigDecimal("-25.78"), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal("-31.03"), totals.getGoodsTotalGross());
        assertEquals(BigDecimal.ZERO, totals.getTotalDiscountNet());
        assertEquals(BigDecimal.ZERO, totals.getTotalDiscountGross());
        assertEquals(new BigDecimal("-35.78"), totals.getGrandTotalNet());
        assertEquals(new BigDecimal("-42.93"), totals.getGrandTotalGross());
        assertEquals(new BigDecimal("-42.93"), totals.getPaymentTotal());
        assertEquals(new BigDecimal("-10.00"), totals.getShippingCostNet());
        assertEquals(new BigDecimal("-11.90"), totals.getShippingCostGross());

        List<GrandTotalTaxes> grandTotalTaxes = totals.getGrandTotalTaxes();
        assertEquals(2, grandTotalTaxes.size());
        GrandTotalTaxes grandTotalTax = grandTotalTaxes.stream().filter(tax -> tax.getRate().equals(new BigDecimal("19.00"))).findFirst().orElse(null);
        assertNotNull(grandTotalTax);
        assertEquals(new BigDecimal("-0.19"), grandTotalTax.getValue());
        grandTotalTax = grandTotalTaxes.stream().filter(tax -> tax.getRate().equals(new BigDecimal("20.00"))).findFirst().orElse(null);
        assertNotNull(grandTotalTax);
        assertEquals(new BigDecimal("-6.96"), grandTotalTax.getValue());
    }

    private void checkEventIsPublished(SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage) {

        verify(salesOrderService).getOrderByOrderNumber("580309129");
        verify(salesOrderReturnService).handleSalesOrderReturn(salesCreditNoteCreatedMessage, RETURN_ORDER_CREATED, CORE_CREDIT_NOTE_CREATED);
        verify(snsPublishService).publishReturnOrderCreatedEvent(argThat(
                salesOrderReturn -> {
                    assertThat(salesOrderReturn.getOrderNumber()).isEqualTo("RO-876130");
                    assertThat(salesOrderReturn.getOrderGroupId()).isEqualTo("580309129");
                    return true;
                }
        ));
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreated() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));
        bpmUtil.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);

        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName()));

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        verify(camundaHelper).correlateMessage(eq(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED),
                argThat(order -> StringUtils.equals(order.getOrderNumber(), originalOrderNumber)));

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "12.95",
                "10.79",
                "12.95",
                "10.79",
                "12.95",
                "0",
                "0");

        verifyThatNewRelicIsCalled(newOrderNumberCreatedInSoh);

        assertTrue(bpmUtil.isProcessEnded(orderProcess));
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWaitingOnOrderPaymentSecured() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String originalOrderNumber = salesOrder.getOrderNumber();

        var message = getObjectByResource("coreSalesInvoiceCreatedOneItem.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);

        assertFalse(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_CORE_SALES_INVOICE_CREATED.getName()));

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        verify(camundaHelper, never()).correlateMessage(eq(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED),
                argThat(order -> StringUtils.equals(order.getOrderNumber(), originalOrderNumber)));

        assertFalse(bpmUtil.isProcessEnded(orderProcess));
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreateWithMultipleItemsSameSkus() {

        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));
        bpmUtil.sendMessage(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName(), salesOrder.getOrderNumber());

        String originalOrderNumber = salesOrder.getOrderNumber();
        var message = getObjectByResource("coreSalesInvoiceCreatedMultipleItemsSameSkus.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> checkIfInvoiceCreatedReceivedProcessExists(newOrderNumberCreatedInSoh)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "834.52",
                "695.64",
                "847.60",
                "706.63",
                "847.60",
                "13.08",
                "10.99");

        verifyThatNewRelicIsCalled(newOrderNumberCreatedInSoh);
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWithMultipleItems() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String originalOrderNumber = salesOrder.getOrderNumber();
        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";

        var message = getObjectByResource("coreSalesInvoiceCreatedMultipleItems.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        assertTrue(timedPollingService.pollWithDefaultTiming(() -> checkIfInvoiceCreatedReceivedProcessExists(newOrderNumberCreatedInSoh)));
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "445.60",
                "371.63",
                "445.60",
                "13.08",
                "10.99");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWithNotConsolidatedDuplicateItems() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        String originalOrderNumber = salesOrder.getOrderNumber();
        OrderRows duplicatedOrderRow = objectUtil.deepCopyOf(salesOrder.getLatestJson().getOrderRows().get(0), OrderRows.class);
        salesOrder.getLatestJson().getOrderRows().get(0).setQuantity(new BigDecimal("2"));
        duplicatedOrderRow.setRowKey(3);
        duplicatedOrderRow.setQuantity(new BigDecimal("3"));
        salesOrder.getLatestJson().getOrderRows().add(duplicatedOrderRow);
        salesOrderService.save(salesOrder, ORDER_CREATED);

        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";

        var message = getObjectByResource("coreSalesInvoiceCreatedMultipleItems.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "445.60",
                "371.63",
                "445.60",
                "13.08",
                "10.99");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);

        SalesOrder originalSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        List<OrderRows> orderRows = originalSalesOrder.getLatestJson().getOrderRows();
        assertEquals(3, orderRows.size());
        assertEquals(2, orderRows.stream().filter(OrderRows::getIsCancelled).count());
    }

    @Test
    void testQueueListenerCoreSalesInvoiceCreatedWithConsolidatedItems() {

        var salesOrder = salesOrderUtil.createNewSalesOrder();
        String originalOrderNumber = salesOrder.getOrderNumber();
        OrderRows duplicatedOrderRow = objectUtil.deepCopyOf(salesOrder.getLatestJson().getOrderRows().get(0), OrderRows.class);
        duplicatedOrderRow.setRowKey(3);
        salesOrder.getLatestJson().getOrderRows().add(duplicatedOrderRow);
        salesOrderService.save(salesOrder, ORDER_CREATED);

        final ProcessInstance orderProcess = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);
        assertTrue(bpmUtil.isProcessWaitingAtExpectedToken(orderProcess, MSG_ORDER_PAYMENT_SECURED.getName()));

        String invoiceNumber = "10";
        String rowSku1 = "1440-47378";
        String rowSku2 = "2010-10183";
        String rowSku3 = "2022-KBA";
        var message = getObjectByResource("coreSalesInvoiceCreatedMultipleItems.json", CoreSalesInvoiceCreatedMessage.class);
        var messageWrapper = MessageWrapper.builder().build();

        //Replace order number with randomly created order number as expected
        message.getSalesInvoice().getSalesInvoiceHeader().setOrderNumber(originalOrderNumber);

        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);

        String newOrderNumberCreatedInSoh = createOrderNumberInSOH(originalOrderNumber, invoiceNumber);
        checkTotalsValues(newOrderNumberCreatedInSoh,
                "432.52",
                "360.64",
                "445.60",
                "371.63",
                "445.60",
                "13.08",
                "10.99");
        checkOrderRows(newOrderNumberCreatedInSoh, rowSku1, rowSku2, rowSku3);

        SalesOrder originalSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        originalSalesOrder.getLatestJson().getOrderRows().stream().map(OrderRows::getIsCancelled).forEach(Assertions::assertTrue);

        SalesOrder subsequentSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber() + ORDER_NUMBER_SEPARATOR + invoiceNumber).orElseThrow();
        assertNotNull(subsequentSalesOrder.getInvoiceEvent());
        assertNotNull(subsequentSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        assertNotNull(subsequentSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber());
        assertEquals(subsequentSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId(),
                subsequentSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderGroupId());
        assertEquals(subsequentSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber(),
                subsequentSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
    }

    private boolean checkIfInvoiceCreatedReceivedProcessExists(String newOrderNumberCreatedInSoh) {
        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(newOrderNumberCreatedInSoh).orElse(null);
        if (updatedOrder == null || updatedOrder.getId() == null)
            return false;
        return !runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(updatedOrder.getId().toString())
                .variableValueEquals(ORDER_NUMBER.getName(), updatedOrder.getOrderNumber())
                .variableValueEquals(ORDER_GROUP_ID.getName(), updatedOrder.getOrderGroupId())
                .list().isEmpty();
    }

    private void checkTotalsValues(String orderNumber,
                                   String goodsTotalGross,
                                   String goodsTotalNet,
                                   String grandTotalGross,
                                   String grandTotalNet,
                                   String paymentTotal,
                                   String shippingGross,
                                   String shippingNet) {

        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        Totals totals = updatedOrder.getLatestJson().getOrderHeader().getTotals();
        assertEquals(new BigDecimal(goodsTotalGross), totals.getGoodsTotalGross());
        assertEquals(new BigDecimal(goodsTotalNet), totals.getGoodsTotalNet());
        assertEquals(new BigDecimal("0"), totals.getTotalDiscountGross());
        assertEquals(new BigDecimal("0"), totals.getTotalDiscountNet());
        assertEquals(new BigDecimal(grandTotalGross), totals.getGrandTotalGross());
        assertEquals(new BigDecimal(grandTotalNet), totals.getGrandTotalNet());
        assertEquals(new BigDecimal(paymentTotal), totals.getPaymentTotal());
        assertEquals(new BigDecimal(shippingGross), totals.getShippingCostGross());
        assertEquals(new BigDecimal(shippingNet), totals.getShippingCostNet());
        assertNotNull(totals.getSurcharges());
        assertNull(totals.getSurcharges().getDepositGross());
        assertNull(totals.getSurcharges().getDepositNet());
        assertNull(totals.getSurcharges().getBulkyGoodsGross());
        assertNull(totals.getSurcharges().getBulkyGoodsNet());
        assertNull(totals.getSurcharges().getRiskyGoodsGross());
        assertNull(totals.getSurcharges().getRiskyGoodsNet());
        assertNull(totals.getSurcharges().getPaymentGross());
        assertNull(totals.getSurcharges().getPaymentNet());
    }

    private void checkOrderRows(String orderNumber, String sku1, String sku2, String sku3) {
        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(orderNumber).orElse(null);
        assertNotNull(updatedOrder);
        List<OrderRows> orderRows = updatedOrder.getLatestJson().getOrderRows();
        checkOrderRowValues(
                orderRows.get(0),
                sku1,
                "2",
                "20.00",
                UnitValues.builder()
                        .goodsValueGross(new BigDecimal("201.00"))
                        .goodsValueNet(new BigDecimal("167.50"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .discountedGross(new BigDecimal("201.00"))
                        .discountedNet(new BigDecimal("167.50"))
                        .build(),
                SumValues.builder()
                        .goodsValueGross(new BigDecimal("402.00"))
                        .goodsValueNet(new BigDecimal("335.00"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .totalDiscountedGross(new BigDecimal("402.00"))
                        .totalDiscountedNet(new BigDecimal("335.00"))
                        .build(),
                "Unterdruckpumpe, Bremsanlage");
        checkOrderRowValues(
                orderRows.get(1),
                sku2,
                "2",
                "19.00",
                UnitValues.builder()
                        .goodsValueGross(new BigDecimal("10.00"))
                        .goodsValueNet(new BigDecimal("8.40"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .discountedGross(new BigDecimal("10.00"))
                        .discountedNet(new BigDecimal("8.40"))
                        .build(),
                SumValues.builder()
                        .goodsValueGross(new BigDecimal("20.00"))
                        .goodsValueNet(new BigDecimal("16.80"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .totalDiscountedGross(new BigDecimal("20.00"))
                        .totalDiscountedNet(new BigDecimal("16.80"))
                        .build(),
                "Luftfilter");
        checkOrderRowValues(
                orderRows.get(2),
                sku3,
                "1",
                "19.00",
                UnitValues.builder()
                        .goodsValueGross(new BigDecimal("10.52"))
                        .goodsValueNet(new BigDecimal("8.84"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .discountedGross(new BigDecimal("10.52"))
                        .discountedNet(new BigDecimal("8.84"))
                        .build(),
                SumValues.builder()
                        .goodsValueGross(new BigDecimal("10.52"))
                        .goodsValueNet(new BigDecimal("8.84"))
                        .discountGross(BigDecimal.ZERO)
                        .discountNet(BigDecimal.ZERO)
                        .totalDiscountedGross(new BigDecimal("10.52"))
                        .totalDiscountedNet(new BigDecimal("8.84"))
                        .build(),
                sku3);
    }

    private void checkOrderRowValues(OrderRows row, String sku, String quantity, String taxRate,
                                     UnitValues expectedUnitValues, SumValues expectedSumValues, String description) {
        assertEquals(sku, row.getSku());
        assertEquals(new BigDecimal(quantity), row.getQuantity());
        assertEquals(new BigDecimal(taxRate), row.getTaxRate());
        assertEquals("shipment_regular", row.getShippingType());
        assertEquals(expectedUnitValues.getGoodsValueGross(), row.getUnitValues().getGoodsValueGross());
        assertEquals(expectedUnitValues.getGoodsValueNet(), row.getUnitValues().getGoodsValueNet());
        assertEquals(expectedUnitValues.getDiscountedGross(), row.getUnitValues().getDiscountedGross());
        assertEquals(expectedUnitValues.getDiscountedNet(), row.getUnitValues().getDiscountedNet());
        assertEquals(expectedSumValues.getGoodsValueGross(), row.getSumValues().getGoodsValueGross());
        assertEquals(expectedSumValues.getGoodsValueNet(), row.getSumValues().getGoodsValueNet());
        assertEquals(expectedSumValues.getTotalDiscountedGross(), row.getSumValues().getTotalDiscountedGross());
        assertEquals(expectedSumValues.getTotalDiscountedNet(), row.getSumValues().getTotalDiscountedNet());
        assertEquals(description, row.getName());
    }

    private void verifyThatNewRelicIsCalled(String newOrderNumberCreatedInSoh) {
        SalesOrder updatedOrder = salesOrderService.getOrderByOrderNumber(newOrderNumberCreatedInSoh).orElse(null);
        Map<String, Object> eventAttributes = new HashMap<>();
        assertThat(updatedOrder).isNotNull();
        var latestJson = updatedOrder.getLatestJson();
        eventAttributes.put("Platform", latestJson.getOrderHeader().getPlatform().name());
        eventAttributes.put("SalesChannel", latestJson.getOrderHeader().getSalesChannel());
        eventAttributes.put("SalesOrderNumber", latestJson.getOrderHeader().getOrderNumber());

        verify(insights).recordCustomEvent(eq("SohSubsequentOrderGenerated"),
                argThat(map -> {
                    assertThat((Map<String, Object>) map).containsAllEntriesOf(eventAttributes);
                    return true;
                }));
    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> bpmUtil.cleanUp());
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        invoiceNumberCounterService.init();
    }
}
