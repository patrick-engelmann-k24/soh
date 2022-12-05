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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.INVOICING_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static de.kfzteile24.salesOrderHub.helper.SalesOrderUtil.createNewSalesOrderV3;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

class AggregateInvoiceDataDelegateIntegrationTest extends AbstractIntegrationTest {

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


        doReturn(null).when(camundaHelper).correlateDropshipmentOrderCancelledMessage(any());
        doReturn(null).when(camundaHelper).correlateDropshipmentOrderFullyInvoicedMessage(any());

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

        Map<String, Object> processVariables = new HashMap<>();
//        processVariables.put(IS_ORDER_CANCELLED.getName(), false);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(INVOICING_PROCESS.getName(), processVariables);

        assertTrue(pollingService.pollWithDefaultTiming(() -> {
            assertThat(processInstance).hasPassed("eventStartTimerInvoicingProcess");
            return true;
        }));
        List<SalesOrder> fullyOrderList = salesOrderRepository.findAllByOrderGroupIdOrderByUpdatedAtDesc(salesOrderFullyInvoiced.getOrderGroupId());
        Assertions.assertThat(fullyOrderList).hasSize(1);
        Assertions.assertThat(fullyOrderList.get(0).getOrderNumber()).isEqualTo("123456789");
        Assertions.assertThat(fullyOrderList.get(0).getInvoiceEvent()).isNotNull();
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

    @AfterEach
    public void cleanup() {
        pollingService.retry(() -> bpmUtil.cleanUp());
        pollingService.retry(() -> salesOrderRepository.deleteAll());
        pollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAll());
    }
}
