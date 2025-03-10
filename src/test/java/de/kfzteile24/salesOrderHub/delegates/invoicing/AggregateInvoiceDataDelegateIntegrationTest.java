package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentOrderRow;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.DropshipmentHelper;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.INVOICING_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.AGGREGATE_INVOICE_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_SUBSEQUENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Gateways.XOR_CHECK_PARTIAL_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

class AggregateInvoiceDataDelegateIntegrationTest extends AbstractIntegrationTest {

    private String invoiceNumber;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @Autowired
    private DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;

    @Autowired
    private DropshipmentOrderRowRepository dropshipmentOrderRowRepository;

    @Autowired
    private TimedPollingService pollingService;

    @Autowired
    private BpmUtil bpmUtil;

    @Autowired
    private DropshipmentHelper dropshipmentHelper;

    @Test
    void testSubprocessCreationAndSubsequentOrderCreation() {

        prepareTestData();
        doReturn(messageCorrelationResult).when(camundaHelper).correlateMessage(any(Messages.class), anyString());

        ProcessInstance processInstance = camundaHelper.startProcessByProcessDefinition(INVOICING_PROCESS);

        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(processInstance).hasPassedInOrder(
                    "eventStartTimerInvoicingProcess",
                    AGGREGATE_INVOICE_DATA.getName(),
                    "eventStartSubInvoicing",
                    XOR_CHECK_PARTIAL_INVOICE.getName(),
                    INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE.getName()
                    );
            return true;
        }));

        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(processInstance).hasPassed(INVOICING_CREATE_SUBSEQUENT_ORDER.getName());
            return true;
        }));

        verifyIfOrderFullyInvoiced();
        verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation();
        verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation();
        verifyIfOrderPartiallyInvoicedAndOrderFullyCancelled();
    }

    private void prepareTestData() {
        // Fully Invoiced
        final SalesOrder salesOrderFullyInvoiced =
                getSalesOrder("123456789", "123456789", false);

        // Partially Invoiced ({-1} is added to order number for subsequent order)
        final SalesOrder salesOrderPartiallyInvoiced1 =
                getSalesOrder("423456789", "423456789", false);

        // Partially Invoiced ({-1} is increased in order number for subsequent order)
        final SalesOrder salesOrderPartiallyInvoiced2 =
                getSalesOrder("523456789", "523456789", false);
        getSalesOrder("523456789-1", "523456789", false);

        // Partially Invoiced (fully cancelled)
        final SalesOrder salesOrderPartiallyInvoiced3 =
                getSalesOrder("623456789", "623456789", true);

        List<DropshipmentInvoiceRow> dropshipmentInvoiceRowList = List.of(
            dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderFullyInvoiced.getOrderNumber(), 3),
            dropshipmentHelper.createDropshipmentInvoiceRow("sku-2", salesOrderFullyInvoiced.getOrderNumber(), 3),
            dropshipmentHelper.createDropshipmentInvoiceRow("sku-3", salesOrderFullyInvoiced.getOrderNumber(), 3),

            dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderPartiallyInvoiced1.getOrderNumber(), 3),

            dropshipmentHelper.createDropshipmentInvoiceRow("sku-1", salesOrderPartiallyInvoiced2.getOrderNumber(), 3),
            dropshipmentHelper.createDropshipmentInvoiceRow("sku-2", salesOrderPartiallyInvoiced2.getOrderNumber(), 3),
            dropshipmentHelper.createDropshipmentInvoiceRow("sku-3", salesOrderPartiallyInvoiced2.getOrderNumber(), 1),

            dropshipmentHelper.createDropshipmentInvoiceRow("sku-2", salesOrderPartiallyInvoiced3.getOrderNumber(), 3),
            dropshipmentHelper.createDropshipmentInvoiceRow("sku-3", salesOrderPartiallyInvoiced3.getOrderNumber(), 3)
        );
        dropshipmentInvoiceRowRepository.saveAll(dropshipmentInvoiceRowList);


        List<DropshipmentOrderRow> dropshipmentOrderRowList = List.of(
                DropshipmentOrderRow.builder().orderNumber("623456789").sku("sku-1").quantity(3).quantityShipped(3).build(),
                DropshipmentOrderRow.builder().orderNumber("623456789").sku("sku-2").quantity(3).quantityShipped(3).build(),
                DropshipmentOrderRow.builder().orderNumber("623456789").sku("sku-3").quantity(3).quantityShipped(4).build()
        );
        dropshipmentOrderRowRepository.saveAll(dropshipmentOrderRowList);
    }

    @NotNull
    private SalesOrder getSalesOrder(String orderNumber, String orderGroupId, boolean isFullyCancelled) {
        final var salesOrder = createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber(orderNumber);
        salesOrder.setOrderGroupId(orderGroupId);
        Order order = salesOrder.getLatestJson();
        order.getOrderHeader().setOrderNumber(orderNumber);
        order.getOrderHeader().setOrderGroupId(orderGroupId);
        if (isFullyCancelled) {
            order.getOrderRows().stream().filter(row -> row.getSku().equals("sku-1")).findFirst().ifPresent(row -> row.setIsCancelled(true));
        }
        order.getOrderRows().forEach(row -> row.setQuantity(BigDecimal.valueOf(3)));
        salesOrder.setLatestJson(order);
        salesOrder.setOriginalOrder(order);
        return salesOrderRepository.save(salesOrder);
    }

    private void verifyIfOrderFullyInvoiced() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("123456789");
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(salesOrders).hasSize(1);
            softly.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo("123456789");
            softly.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
            softly.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
            softly.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("123456789");
            softly.assertThat(salesOrders.get(0).isCancelled()).isFalse();
        }
        invoiceNumber = getInvoiceNumber(salesOrders.get(0));
    }

    private void verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("423456789");
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(salesOrders).hasSize(2);
            var original = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("423456789"))
                    .findFirst();
            softly.assertThat(original).isPresent();
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
            softly.assertThat(original.get().getInvoiceEvent()).isNull();
            softly.assertThat(original.get().isCancelled()).isFalse();
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);
            var subsequent = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("423456789-1"))
                    .findFirst();
            softly.assertThat(subsequent).isPresent();
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(nextInvoiceNumber());
            softly.assertThat(subsequent.get().getInvoiceEvent()).isNotNull();
            softly.assertThat(subsequent.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("423456789-1");
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));
        }
    }

    private void verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("523456789");
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(salesOrders).hasSize(3);
            var original = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("523456789"))
                    .findFirst();
            softly.assertThat(original).isPresent();
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
            softly.assertThat(original.get().getInvoiceEvent()).isNull();
            softly.assertThat(original.get().isCancelled()).isFalse();
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);
            var subsequent1 = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("523456789-1"))
                    .findFirst();
            softly.assertThat(subsequent1).isPresent();
            softly.assertThat(subsequent1.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
            softly.assertThat(subsequent1.get().getInvoiceEvent()).isNull();
            var subsequent2 = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("523456789-2"))
                    .findFirst();
            softly.assertThat(subsequent2).isPresent();
            softly.assertThat(subsequent2.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
            softly.assertThat(subsequent2.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(nextInvoiceNumber());
            softly.assertThat(subsequent2.get().getInvoiceEvent()).isNotNull();
            softly.assertThat(subsequent2.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("523456789-2");
            softly.assertThat(subsequent2.get().getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
            softly.assertThat(subsequent2.get().getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));
        }
    }

    private void verifyIfOrderPartiallyInvoicedAndOrderFullyCancelled() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("623456789");
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            softly.assertThat(salesOrders).hasSize(2);
            Optional<SalesOrder> original = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("623456789"))
                    .findFirst();
            softly.assertThat(original).isPresent();
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
            softly.assertThat(original.get().getInvoiceEvent()).isNull();
            softly.assertThat(original.get().isCancelled()).isTrue();
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(original.get().getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(BigDecimal.ZERO);
            Optional<SalesOrder> subsequent = salesOrders.stream()
                    .filter(salesOrder -> salesOrder.getOrderNumber().equals("623456789-1"))
                    .findFirst();
            softly.assertThat(subsequent).isPresent();
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(nextInvoiceNumber());
            softly.assertThat(subsequent.get().getInvoiceEvent()).isNotNull();
            softly.assertThat(subsequent.get().getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("623456789-1");
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getTotals().getShippingCostGross()).isEqualTo(new BigDecimal(100));
            softly.assertThat(subsequent.get().getLatestJson().getOrderHeader().getTotals().getShippingCostNet()).isEqualTo(new BigDecimal(80));
        }
    }

    private String nextInvoiceNumber() {
        int lastFiveDigitIndex = invoiceNumber.length() - 5;
        int lastFiveDigitNumber = Integer.parseInt(this.invoiceNumber.substring(lastFiveDigitIndex));
        invoiceNumber = this.invoiceNumber.substring(0, lastFiveDigitIndex) + String.format("%05d", (lastFiveDigitNumber + 1));
        return invoiceNumber;
    }

    private String getInvoiceNumber(SalesOrder salesOrder) {
        Assertions.assertThat(salesOrder.getInvoiceEvent()).isNotNull();
        return salesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber();
    }

    @BeforeEach
    public void prepare() {
        pollingService.retry(() -> bpmUtil.cleanUp());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAll());
        pollingService.retry(() -> dropshipmentOrderRowRepository.deleteAll());
    }

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> bpmUtil.cleanUp());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAll());
        pollingService.retry(() -> dropshipmentOrderRowRepository.deleteAll());
    }
}
