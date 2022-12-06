package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.soh.order.dto.Order;
import org.assertj.core.api.Assertions;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

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
import static org.mockito.Mockito.doReturn;

class AggregateInvoiceDataDelegateIntegrationTest extends AbstractIntegrationTest {

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

    @Test
    void testSubprocessCreationAndSubsequentOrderCreation() {

        prepareTestData();
        doReturn(null).when(camundaHelper).correlateDropshipmentOrderCancelledMessage(any());
        doReturn(null).when(camundaHelper).correlateDropshipmentOrderFullyInvoicedMessage(any());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(INVOICING_PROCESS.getName());

        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(processInstance).hasPassedInOrder(
                    "eventStartTimerInvoicingProcess",
                    AGGREGATE_INVOICE_DATA.getName(),
                    "eventStartSubInvoicing",
                    XOR_CHECK_PARTIAL_INVOICE.getName(),
                    INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE.getName(),
                    INVOICING_CREATE_SUBSEQUENT_ORDER.getName()
                    );
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

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderFullyInvoiced.getOrderNumber())
                .sku("sku-1")
                .build(),

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderFullyInvoiced.getOrderNumber())
                .sku("sku-2")
                .build(),

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderFullyInvoiced.getOrderNumber())
                .sku("sku-3")
                .build(),

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderPartiallyInvoiced1.getOrderNumber())
                .sku("sku-1")
                .build(),

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderPartiallyInvoiced2.getOrderNumber())
                .sku("sku-1")
                .build(),

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderPartiallyInvoiced3.getOrderNumber())
                .sku("sku-2")
                .build(),

        DropshipmentInvoiceRow.builder()
                .orderNumber(salesOrderPartiallyInvoiced3.getOrderNumber())
                .sku("sku-3")
                .build()
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

    private void verifyIfOrderFullyInvoiced() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("123456789");
        Assertions.assertThat(salesOrders).hasSize(1);
        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo("123456789");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("123456789");
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isFalse();
        invoiceNumber = getInvoiceNumber(salesOrders.get(0));
    }

    private void verifyIfOrderPartiallyInvoicedAndFirstSubsequentOrderNumberCreation() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("423456789");
        Assertions.assertThat(salesOrders).hasSize(2);
        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo("423456789");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo("423456789-1");
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(nextInvoiceNumber());
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("423456789-1");
    }

    private void verifyIfOrderPartiallyInvoicedAndSecondSubsequentOrderNumberCreation() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("523456789");
        Assertions.assertThat(salesOrders).hasSize(3);
        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo("523456789");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isFalse();
        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo("523456789-2");
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(nextInvoiceNumber());
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("523456789-2");
        Assertions.assertThat(salesOrders.get(2).getOrderNumber()).isEqualTo("523456789-1");
        Assertions.assertThat(salesOrders.get(2).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(2).getInvoiceEvent()).isNull();
    }

    private void verifyIfOrderPartiallyInvoicedAndOrderFullyCancelled() {
        var salesOrders = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc("623456789");
        Assertions.assertThat(salesOrders).hasSize(2);
        Assertions.assertThat(salesOrders.get(0).getOrderNumber()).isEqualTo("623456789");
        Assertions.assertThat(salesOrders.get(0).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNull();
        Assertions.assertThat(salesOrders.get(0).getInvoiceEvent()).isNull();
        Assertions.assertThat(salesOrders.get(0).isCancelled()).isTrue();
        Assertions.assertThat(salesOrders.get(1).getOrderNumber()).isEqualTo("623456789-1");
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getLatestJson().getOrderHeader().getDocumentRefNumber()).isEqualTo(nextInvoiceNumber());
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent()).isNotNull();
        Assertions.assertThat(salesOrders.get(1).getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getOrderNumber()).isEqualTo("623456789-1");
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

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> bpmUtil.cleanUp());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAll());
    }
}
