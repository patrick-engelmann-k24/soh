package de.kfzteile24.salesOrderHub.services.invoicing;

import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentOrderRowRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentShipmentService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import lombok.val;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.INVOICING_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.AGGREGATE_INVOICE_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.CREATE_DROPSHIPMENT_SUBSEQUENT_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_CANCEL_DROPSHIPMENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_GENERATE_PARTLY_INVOICED_PDF;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PUBLISH_PARTLY_INVOICED_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_SUBSEQUENT_ORDER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.PERSIST_DROPSHIPMENT_ORDER_ITEMS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropshipmentOrderPartiallyInvoicedFullyCancelledIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentShipmentService dropshipmentShipmentService;
    @Autowired
    private SalesOrderRepository salesOrderRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private InvoiceNumberCounterService invoiceNumberCounterService;
    @Autowired
    private InvoiceNumberCounterRepository invoiceNumberCounterRepository;
    @Autowired
    private BpmUtil bpmUtil;
    @Autowired
    private TimedPollingService timerService;
    @Autowired
    private TimedPollingService timedPollingService;
    @Autowired
    protected DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @Autowired
    protected DropshipmentInvoiceRowRepository dropshipmentInvoiceRowRepository;
    @Autowired
    protected DropshipmentOrderRowService dropshipmentOrderRowService;
    @Autowired
    protected DropshipmentOrderRowRepository dropshipmentOrderRowRepository;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private SalesOrderProcessService salesOrderProcessService;

    @BeforeEach
    public void setup() {
        super.setUp();
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        timedPollingService.retry(() -> invoiceNumberCounterService.init());
        timedPollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
        timedPollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAllInBatch());
        timedPollingService.retry(() -> dropshipmentOrderRowRepository.deleteAllInBatch());
    }

    @Test
    void testHandleDropShipmentOrderShipmentConfirmed(){
        var salesOrder = createDropshipmentSalesOrder();
        assertThat(dropshipmentInvoiceRowService.getByOrderNumber(salesOrder.getOrderNumber()).size()).isZero();
        var message = createPartialShipmentConfirmedMessage(salesOrder);

        var processInstance1 = startDropshipmentConfirmedProcess(salesOrder, message);

        startPartialInvoicingFullyCancelledProcess();

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance1.getId(), END_MSG_ORDER_CANCELLED.getName())));

        var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(salesOrder.getOrderNumber()).orElseThrow();
        assertThat(updatedSalesOrder.getInvoiceEvent()).isNull();
        assertThat(updatedSalesOrder.isCancelled()).isTrue();

        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().size()).isEqualTo(3);
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(0).getSku()).isEqualTo("sku-1");
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(0).getQuantity().intValue()).isEqualTo(1);
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(1).getSku()).isEqualTo("sku-2");
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(1).getQuantity().intValue()).isEqualTo(6);
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(2).getSku()).isEqualTo("sku-3");
        assertThat(updatedSalesOrder.getLatestJson().getOrderRows().get(2).getQuantity().intValue()).isEqualTo(5);
        assertThat(updatedSalesOrder.isShipped()).isTrue();

        val dropshipmentOrderRow1 = dropshipmentOrderRowRepository.findBySkuAndOrderNumber("sku-1", updatedSalesOrder.getOrderNumber()).get();
        assertThat(dropshipmentOrderRow1.getOrderNumber()).isEqualTo(updatedSalesOrder.getOrderNumber());
        assertThat(dropshipmentOrderRow1.getSku()).isEqualTo("sku-1");
        assertThat(dropshipmentOrderRow1.getQuantity().intValue()).isEqualTo(1);
        assertThat(dropshipmentOrderRow1.getQuantityShipped().intValue()).isEqualTo(1);

        val dropshipmentOrderRow2 = dropshipmentOrderRowRepository.findBySkuAndOrderNumber("sku-2", updatedSalesOrder.getOrderNumber()).get();
        assertThat(dropshipmentOrderRow2.getOrderNumber()).isEqualTo(updatedSalesOrder.getOrderNumber());
        assertThat(dropshipmentOrderRow2.getSku()).isEqualTo("sku-2");
        assertThat(dropshipmentOrderRow2.getQuantity().intValue()).isEqualTo(6);
        assertThat(dropshipmentOrderRow2.getQuantityShipped().intValue()).isEqualTo(6);

        val dropshipmentOrderRow3 = dropshipmentOrderRowRepository.findBySkuAndOrderNumber("sku-3", updatedSalesOrder.getOrderNumber()).get();
        assertThat(dropshipmentOrderRow3.getOrderNumber()).isEqualTo(updatedSalesOrder.getOrderNumber());
        assertThat(dropshipmentOrderRow3.getSku()).isEqualTo("sku-3");
        assertThat(dropshipmentOrderRow3.getQuantity().intValue()).isEqualTo(5);
        assertThat(dropshipmentOrderRow3.getQuantityShipped().intValue()).isEqualTo(7);
    }

    private DropshipmentShipmentConfirmedMessage createPartialShipmentConfirmedMessage(SalesOrder salesOrder) {
        return DropshipmentShipmentConfirmedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .purchaseOrderNumber(salesOrder.getOrderNumber())
                .supplierInternalId(10)
                .shipmentDate(RandomStringUtils.randomAlphabetic(10))
                .items(List.of(
                        ShipmentItem.builder()
                                .productNumber("sku-1")
                                .parcelNumber("00F8F0LT")
                                .trackingLink("http://abc1")
                                .serviceProviderName("abc1")
                                .quantity(1)
                                .build(),
                        ShipmentItem.builder()
                                .productNumber("sku-2")
                                .parcelNumber("00F8F0LT2")
                                .trackingLink("http://abc2")
                                .serviceProviderName("abc2")
                                .quantity(2)
                                .build(),
                        ShipmentItem.builder()
                                .productNumber("sku-2")
                                .parcelNumber("00F8F0LT2")
                                .trackingLink("http://abc2")
                                .serviceProviderName("abc2")
                                .quantity(4)
                                .build(),
                        ShipmentItem.builder()
                                .productNumber("sku-3")
                                .parcelNumber("00F8F0LT32")
                                .trackingLink("http://abc3")
                                .serviceProviderName("abc3")
                                .quantity(3)
                                .build(),
                        ShipmentItem.builder()
                                .productNumber("sku-3")
                                .parcelNumber("00F8F0LT32")
                                .trackingLink("http://abc3")
                                .serviceProviderName("abc3")
                                .quantity(4)
                                .build()))
                .build();
    }

    private SalesOrder createDropshipmentSalesOrder() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        salesOrder.setOrderNumber("213456789");
        salesOrder.setOrderGroupId("213456789");
        ((Order) salesOrder.getLatestJson()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());
        ((Order) salesOrder.getLatestJson()).getOrderRows().stream().filter(row -> row.getSku().equals("sku-1")).findFirst().ifPresent(row -> row.setIsCancelled(true));

        ((Order) salesOrder.getLatestJson()).getOrderRows().stream().filter(r -> "sku-2".equals(r.getSku())).findFirst()
                .ifPresent(r -> r.setQuantity(BigDecimal.valueOf(6)));
        ((Order) salesOrder.getLatestJson()).getOrderRows().stream().filter(r -> "sku-3".equals(r.getSku())).findFirst()
                .ifPresent(r -> r.setQuantity(BigDecimal.valueOf(5)));

        return salesOrderService.save(salesOrder, Action.ORDER_CREATED);
    }

    private ProcessInstance startDropshipmentConfirmedProcess(SalesOrder salesOrder, DropshipmentShipmentConfirmedMessage message) {
        ProcessInstance processInstance = salesOrderProcessService.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(ProcessDefinition.SALES_ORDER_PROCESS, salesOrder.getOrderNumber())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

        var messageCorrelationResult = bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true));

        assertThat(messageCorrelationResult.getExecution().getProcessInstanceId()).isNotBlank();

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), PERSIST_DROPSHIPMENT_ORDER_ITEMS.getName())));
        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.isProcessWaitingAtExpectedToken(processInstance, MSG_DROPSHIPMENT_ORDER_FULLY_COMPLETED.getName())));

        dropshipmentShipmentService.handleDropshipmentShipmentConfirmed(message, messageWrapper);

        //small hack to simulate partial invoice in invoicing process
        //other solution would be to create 2 additional processes (which is more time-consuming)
        val dropshipmentInvoiceRow = dropshipmentInvoiceRowService.getBySkuAndOrderNumber("sku-1", salesOrder.getOrderNumber()).orElseThrow();
        dropshipmentInvoiceRowRepository.delete(dropshipmentInvoiceRow);

        return processInstance;
    }

    @SneakyThrows
    private void startPartialInvoicingFullyCancelledProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(INVOICING_PROCESS.getName());

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(),AGGREGATE_INVOICE_DATA.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), INVOICING_CREATE_SUBSEQUENT_ORDER.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), CREATE_DROPSHIPMENT_SUBSEQUENT_INVOICE.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PUBLISH_PARTLY_INVOICED_DATA.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_GENERATE_PARTLY_INVOICED_PDF.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                bpmUtil.hasPassed(processInstance.getId(), EVENT_THROW_MSG_CANCEL_DROPSHIPMENT_ORDER.getName())));

    }

    @AfterEach
    @SneakyThrows
    public void cleanup() {
        timedPollingService.retry(() -> auditLogRepository.deleteAll());
        timedPollingService.retry(() -> salesOrderRepository.deleteAll());
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        timedPollingService.retry(() -> invoiceNumberCounterService.init());
    }
}