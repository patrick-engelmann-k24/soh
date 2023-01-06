package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.InvoiceSavedDelegate;
import de.kfzteile24.salesOrderHub.delegates.salesOrder.SaveInvoiceDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SUBSEQUENT_ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class InvoicingDelegatesMultithreadedIntegrationTest extends AbstractIntegrationTest {

    private String invoiceNumber;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;

    @Autowired
    private SalesOrderInvoiceRepository salesOrderInvoiceRepository;

    @Autowired
    private TimedPollingService pollingService;

    @Autowired
    private BpmUtil bpmUtil;

    @Autowired
    private CreateDropshipmentSubsequentOrderDelegate createDropshipmentSubsequentOrderDelegate;

    @Autowired
    private SaveInvoiceDelegate saveInvoiceDelegate;

    @Autowired
    private InvoiceSavedDelegate invoiceSavedDelegate;

    @Autowired
    private CreateDropshipmentSubsequentInvoiceDelegate createDropshipmentSubsequentInvoiceDelegate;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private DropshipmentHelper dropshipmentHelper;

    @BeforeEach
    public void setup() {
        pollingService.retry(() -> bpmUtil.cleanUp());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAll());
        pollingService.retry(() -> salesOrderInvoiceRepository.deleteAll());
    }

    @Test
    @SneakyThrows
    void testOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation() {


        invoiceNumber = invoiceService.createInvoiceNumber();

        int numberOfOrders = 10;
        List<Pair<Pair<String, String>, Pair<SalesOrder, SalesOrder>>> orders = new ArrayList<>();
        for (int i = 0; i < numberOfOrders; i++) {
            val firstInvoiceNumber = nextInvoiceNumber();
            val secondInvoiceNumber = nextInvoiceNumber();
            orders.add(Pair.of(Pair.of(firstInvoiceNumber, secondInvoiceNumber),
                    Pair.of(createSalesOrderWithRandomOrderNumber(), createSalesOrderWithRandomOrderNumber())));
        }
        for (Pair<Pair<String,String>, Pair<SalesOrder, SalesOrder>> pair : orders) {
            var invoicePair = pair.getLeft();
            var orderPair = pair.getRight();
            preparePartialInvoiceTestData(orderPair.getLeft(), orderPair.getRight(), invoicePair.getLeft(), invoicePair.getRight());
        }

        List<Thread> threads = new ArrayList<>();

        for (Pair<Pair<String,String>, Pair<SalesOrder, SalesOrder>> pair : orders) {

            Thread thread = new Thread(() -> {
                try {
                    var invoicePair = pair.getLeft();
                    var orderPair = pair.getRight();
                    var firstOrder = orderPair.getLeft();
                    var secondOrder = orderPair.getRight();
                    var firstInvoiceNumber = invoicePair.getLeft();
                    var secondInvoiceNumber = invoicePair.getRight();
                    var firstOrderNumber = firstOrder.getOrderNumber();
                    var secondOrderNumber = secondOrder.getOrderNumber();

                    test(firstOrderNumber, secondOrderNumber, firstInvoiceNumber, secondInvoiceNumber);

                } catch (Exception e) {
                    log.error(e.getMessage());
                    throw e;
                }

            });

            threads.add(thread);

        }

        for (Thread thread: threads) {
            thread.start();
        }

        pollingService.poll(Duration.ofMillis(500), Duration.ofSeconds(10), () -> salesOrderRepository.count() == 50);
        pollingService.poll(Duration.ofMillis(500), Duration.ofSeconds(10), () -> salesOrderInvoiceRepository.count() == 20);

        assertTrue(salesOrderRepository.count() == 50);
        assertTrue(salesOrderInvoiceRepository.count() == 20);

        for (Pair<Pair<String,String>, Pair<SalesOrder, SalesOrder>> pair : orders) {
            var invoicePair = pair.getLeft();
            var orderPair = pair.getRight();
            var firstOrder = orderPair.getLeft();
            var secondOrder = orderPair.getRight();
            var firstInvoiceNumber = invoicePair.getLeft();
            var secondInvoiceNumber = invoicePair.getRight();
            var firstOrderNumber = firstOrder.getOrderNumber();
            var secondOrderNumber = secondOrder.getOrderNumber();

            verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation(firstOrderNumber, firstInvoiceNumber);
            verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation(secondOrderNumber, secondInvoiceNumber);
        }

    }

    private void preparePartialInvoiceTestData(SalesOrder salesOrderPartiallyInvoiced1, SalesOrder salesOrderPartiallyInvoiced2,
                                               String firstInvoiceNumber, String secondInvoiceNumber) {

        String secondOrderNumber = salesOrderPartiallyInvoiced2.getOrderNumber();
        getSalesOrder(secondOrderNumber + "-1", secondOrderNumber, false);

        List<DropshipmentInvoiceRow> dropshipmentInvoiceRowList = List.of(
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderPartiallyInvoiced1.getOrderNumber(), firstInvoiceNumber),
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderPartiallyInvoiced2.getOrderNumber(), secondInvoiceNumber)
        );
        dropshipmentInvoiceRowRepository.saveAll(dropshipmentInvoiceRowList);
    }

    @SneakyThrows
    private void test(String firstOrderNumber, String secondOrderNumber, String firstInvoiceNumber, String secondInvoiceNumber)  {

        var delegateExecution = mock(DelegateExecution.class);

        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(firstInvoiceNumber);
        when(delegateExecution.getVariable(SUBSEQUENT_ORDER_NUMBER.getName())).thenReturn(firstOrderNumber + "-1");
        createDropshipmentSubsequentOrderDelegate.execute(delegateExecution);
        createDropshipmentSubsequentInvoiceDelegate.execute(delegateExecution);

        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(secondInvoiceNumber);
        when(delegateExecution.getVariable(SUBSEQUENT_ORDER_NUMBER.getName())).thenReturn(secondOrderNumber + "-2");
        createDropshipmentSubsequentOrderDelegate.execute(delegateExecution);
        createDropshipmentSubsequentInvoiceDelegate.execute(delegateExecution);

        testSubsequentOrder(firstOrderNumber + "-1", firstInvoiceNumber);
        testSubsequentOrder(secondOrderNumber + "-2", secondInvoiceNumber);

    }

    @SneakyThrows
    private void testSubsequentOrder(String orderNumber, String invoiceNumber) {
        var delegateExecution = mock(DelegateExecution.class);
        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(orderNumber);
        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(invoiceNumber);
        when(delegateExecution.getVariable(INVOICE_URL.getName())).thenReturn("s3://staging-k24-invoices/www-k24-de/2022/06/27/" +
                orderNumber + "-" + invoiceNumber + ".pdf");
        when(delegateExecution.getVariable(IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName())).thenReturn(false);

        var thread1 = new Thread(() -> {
            try {
                saveInvoiceDelegate.execute(delegateExecution);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        thread1.start();
        var thread2 = new Thread(() -> {
            try {
                invoiceSavedDelegate.execute(delegateExecution);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });
        thread2.start();

        //TODO: the test fails if saveInvoiceDelegate is called several times in parallel
        //saveInvoiceDelegate.execute(delegateExecution);
        //saveInvoiceDelegate.execute(delegateExecution);

    }

    @NotNull
    private SalesOrder getSalesOrder(String orderNumber, String orderGroupId, boolean isFullyCancelled) {
        final var salesOrderFullyInvoiced = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrderFullyInvoiced.setOrderNumber(orderNumber);
        salesOrderFullyInvoiced.setOrderGroupId(orderGroupId);
        Order order = salesOrderFullyInvoiced.getLatestJson();
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderGroupId);
        if (isFullyCancelled) {
            order.getOrderRows().stream().filter(row -> row.getSku().equals("sku-1")).findFirst().ifPresent(row -> row.setIsCancelled(true));
        }
        salesOrderFullyInvoiced.setLatestJson(order);
        salesOrderFullyInvoiced.setOriginalOrder(order);
        salesOrderRepository.save(salesOrderFullyInvoiced);
        return salesOrderFullyInvoiced;
    }

    private void verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation(String orderNumber, String invoiceNumber) {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(orderNumber);
        Assertions.assertThat(salesOrders).hasSize(2);

        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo(orderNumber + "-1");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(orderNumber + "-1");
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));

        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo(orderNumber);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(1).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);

    }


    private void verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation(String orderNumber, String invoiceNumber) {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(orderNumber);
        Assertions.assertThat(salesOrders).hasSize(3);

        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo(orderNumber + "-2");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(orderNumber + "-2");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));

        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo(orderNumber);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(1).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);

        Assertions.assertThat(salesOrders.get(2).getOrderNumber()).isEqualTo(orderNumber + "-1");
        Assertions.assertThat(salesOrders.get(2).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(2).getInvoiceEvent()).isNull();

    }

    private String nextInvoiceNumber() {
        int lastFiveDigitIndex = invoiceNumber.length() - 5;
        int lastFiveDigitNumber = Integer.parseInt(this.invoiceNumber.substring(lastFiveDigitIndex));
        invoiceNumber = this.invoiceNumber.substring(0, lastFiveDigitIndex) + String.format("%05d", (lastFiveDigitNumber + 1));
        return invoiceNumber;
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> bpmUtil.cleanUp());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAll());
        pollingService.retry(() -> salesOrderInvoiceRepository.deleteAll());
    }

    private SalesOrder createSalesOrderWithRandomOrderNumber() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        String randomOrderNumber = UUID.randomUUID().toString().replace("-", "");
        salesOrder.setOrderNumber(randomOrderNumber);
        salesOrder.setOrderGroupId(randomOrderNumber);
        salesOrder.getLatestJson().getOrderHeader().setOrderNumber(randomOrderNumber);
        salesOrderService.save(salesOrder, Action.ORDER_CREATED);
        return salesOrder;
    }
}
