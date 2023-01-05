package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderCancellationDelegate;
import de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.DropshipmentOrderRowsCancellationDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SUBSEQUENT_ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class CreateDropshipmentSubsequentOrderDelegateIntegrationTest extends AbstractIntegrationTest {

    private static final String FIRST_PARTIAL_INVOICE_ORDER_NUMBER = "423456789";

    private static final String SECOND_PARTIAL_INVOICE_ORDER_NUMBER = "523456789";

    private static final String PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER = "623456789";

    private static final String FULL_INVOICE_ORDER_NUMBER = "623456789";

    private String invoiceNumber;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TimedPollingService pollingService;

    @Autowired
    private BpmUtil bpmUtil;

    @Autowired
    private DropshipmentHelper dropshipmentHelper;

    @Autowired
    private CreateDropshipmentSubsequentOrderDelegate createDropshipmentSubsequentOrderDelegate;

    @Autowired
    private AggregateInvoiceDataDelegate aggregateInvoiceDataDelegate;

    @Autowired
    private DropshipmentOrderStoreInvoiceDelegate dropshipmentOrderStoreInvoiceDelegate;

    @Autowired
    private CreateDropshipmentSubsequentInvoiceDelegate createDropshipmentSubsequentInvoiceDelegate;

    @Autowired
    private DropshipmentOrderRowsCancellationDelegate dropshipmentOrderRowsCancellationDelegate;

    @Autowired
    private DropshipmentOrderCancellationDelegate dropshipmentOrderCancellationDelegate;

    @Autowired
    private InvoiceService invoiceService;

    @Mock
    private DelegateExecution delegateExecution;

    @Test
    @SneakyThrows
    void testOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation() {

        preparePartialInvoiceTestData();

        invoiceNumber = invoiceService.createInvoiceNumber();
        val firstInvoiceNumber = nextInvoiceNumber();
        val secondInvoiceNumber = nextInvoiceNumber();

        aggregateInvoiceDataDelegate.execute(delegateExecution);

        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(firstInvoiceNumber);
        createDropshipmentSubsequentOrderDelegate.execute(delegateExecution);
        when(delegateExecution.getVariable(SUBSEQUENT_ORDER_NUMBER.getName())).thenReturn(FIRST_PARTIAL_INVOICE_ORDER_NUMBER + "-1");
        createDropshipmentSubsequentInvoiceDelegate.execute(delegateExecution);
        verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation(firstInvoiceNumber);

        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(secondInvoiceNumber);
        createDropshipmentSubsequentOrderDelegate.execute(delegateExecution);
        when(delegateExecution.getVariable(SUBSEQUENT_ORDER_NUMBER.getName())).thenReturn(SECOND_PARTIAL_INVOICE_ORDER_NUMBER + "-2");
        createDropshipmentSubsequentInvoiceDelegate.execute(delegateExecution);
        verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation(secondInvoiceNumber);
    }

    @Test
    @SneakyThrows
    void testOrderPartiallyInvoicedAndOrderFullyCancelled() {

        preparePartialInvoiceFullyCancelledTestData();
        doNothing().when(camundaHelper).setVariable(any(), any(), any());

        invoiceNumber = invoiceService.createInvoiceNumber();
        val nextInvoiceNumber = nextInvoiceNumber();

        aggregateInvoiceDataDelegate.execute(delegateExecution);

        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(nextInvoiceNumber);
        createDropshipmentSubsequentOrderDelegate.execute(delegateExecution);
        when(delegateExecution.getVariable(SUBSEQUENT_ORDER_NUMBER.getName())).thenReturn(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER + "-1");
        createDropshipmentSubsequentInvoiceDelegate.execute(delegateExecution);

        when(delegateExecution.getVariable(ORDER_NUMBER.getName())).thenReturn(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER);
        when(delegateExecution.getVariable(ORDER_ROW.getName())).thenReturn("sku-2");
        dropshipmentOrderRowsCancellationDelegate.execute(delegateExecution);
        when(delegateExecution.getVariable(ORDER_ROW.getName())).thenReturn("sku-3");
        dropshipmentOrderRowsCancellationDelegate.execute(delegateExecution);
        dropshipmentOrderCancellationDelegate.execute(delegateExecution);

        verifyIfOrderPartiallyInvoicedAndOrderFullyCancelled(nextInvoiceNumber);
    }

    @Test
    @SneakyThrows
    void testOrderFullyInvoiced() {

        prepareFullInvoiceTestData();

        invoiceNumber = invoiceService.createInvoiceNumber();
        val nextInvoiceNumber = nextInvoiceNumber();

        aggregateInvoiceDataDelegate.execute(delegateExecution);

        when(delegateExecution.getVariable(INVOICE_NUMBER.getName())).thenReturn(nextInvoiceNumber);
        dropshipmentOrderStoreInvoiceDelegate.execute(delegateExecution);
        verifyIfOrderFullyInvoiced(nextInvoiceNumber);
    }

    private void preparePartialInvoiceTestData() {

        // Partially Invoiced ({-1} is added to order number for subsequent order)
        final SalesOrder salesOrderPartiallyInvoiced1 =
                getSalesOrder(FIRST_PARTIAL_INVOICE_ORDER_NUMBER, FIRST_PARTIAL_INVOICE_ORDER_NUMBER, false);

        // Partially Invoiced ({-1} is increased in order number for subsequent order)
        final SalesOrder salesOrderPartiallyInvoiced2 =
                getSalesOrder(SECOND_PARTIAL_INVOICE_ORDER_NUMBER, SECOND_PARTIAL_INVOICE_ORDER_NUMBER, false);
        getSalesOrder(SECOND_PARTIAL_INVOICE_ORDER_NUMBER + "-1", SECOND_PARTIAL_INVOICE_ORDER_NUMBER, false);

        List<DropshipmentInvoiceRow> dropshipmentInvoiceRowList = List.of(
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderPartiallyInvoiced1.getOrderNumber()),
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderPartiallyInvoiced2.getOrderNumber())
        );
        dropshipmentInvoiceRowRepository.saveAll(dropshipmentInvoiceRowList);
    }

    private void preparePartialInvoiceFullyCancelledTestData() {

        // Partially Invoiced (fully cancelled)
        final SalesOrder salesOrderPartiallyInvoiced3 =
                getSalesOrder(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER, PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER, true);

        List<DropshipmentInvoiceRow> dropshipmentInvoiceRowList = List.of(
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-2", salesOrderPartiallyInvoiced3.getOrderNumber()),
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-3", salesOrderPartiallyInvoiced3.getOrderNumber())
        );
        dropshipmentInvoiceRowRepository.saveAll(dropshipmentInvoiceRowList);
    }

    private void prepareFullInvoiceTestData() {
        // Fully Invoiced
        final SalesOrder salesOrderFullyInvoiced =
                getSalesOrder(FULL_INVOICE_ORDER_NUMBER, FULL_INVOICE_ORDER_NUMBER, false);

        List<DropshipmentInvoiceRow> dropshipmentInvoiceRowList = List.of(

                dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderFullyInvoiced.getOrderNumber()),
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-2", salesOrderFullyInvoiced.getOrderNumber()),
                dropshipmentHelper.createDropshipmentInvoiceRow("sku-3", salesOrderFullyInvoiced.getOrderNumber())
        );
        dropshipmentInvoiceRowRepository.saveAll(dropshipmentInvoiceRowList);
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

    private void verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation(String invoiceNumber) {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(FIRST_PARTIAL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders).hasSize(2);
        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo(FIRST_PARTIAL_INVOICE_ORDER_NUMBER + "-1");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(FIRST_PARTIAL_INVOICE_ORDER_NUMBER + "-1");
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));

        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo(FIRST_PARTIAL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(1).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);
    }


    private void verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation(String invoiceNumber) {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(SECOND_PARTIAL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders).hasSize(3);

        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo(SECOND_PARTIAL_INVOICE_ORDER_NUMBER + "-2");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(SECOND_PARTIAL_INVOICE_ORDER_NUMBER + "-2");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));

        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo(SECOND_PARTIAL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(1).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);

        Assertions.assertThat(salesOrders.get(2).getOrderNumber()).isEqualTo(SECOND_PARTIAL_INVOICE_ORDER_NUMBER + "-1");
        Assertions.assertThat(salesOrders.get(2).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(2).getInvoiceEvent()).isNull();

    }

    private void verifyIfOrderPartiallyInvoicedAndOrderFullyCancelled(String invoiceNumber) {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER);
        Assertions.assertThat(salesOrders).hasSize(2);

        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER);
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isTrue();
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);

        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER + "-1");
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(PARTIAL_INVOICE_FULLY_CANCELLED_ORDER_NUMBER + "-1");
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));

    }

    private void verifyIfOrderFullyInvoiced(String invoiceNumber) {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(FULL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders).hasSize(1);
        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo(FULL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo(FULL_INVOICE_ORDER_NUMBER);
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isFalse();
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
    }
}
