package de.kfzteile24.salesOrderHub.services.invoicing;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.kfzteile24.salesOrderHub.AbstractIntegrationTest;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.helper.BpmUtil;
import de.kfzteile24.salesOrderHub.helper.SalesOrderUtil;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import de.kfzteile24.salesOrderHub.repositories.DropshipmentInvoiceRowRepository;
import de.kfzteile24.salesOrderHub.repositories.InvoiceNumberCounterRepository;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderRepository;
import de.kfzteile24.salesOrderHub.services.TimedPollingService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceNumberCounterService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.SneakyThrows;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.variable.Variables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;
import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.INVOICING_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.AGGREGATE_INVOICE_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_INVOICING_DROPSHIPMENT_ORDER_FULLY_INVOICED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_INVOICING_GENERATE_FULLY_INVOICED_PDF;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_INVOICING_PUBLISH_FULLY_INVOICED_DATA;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.END_MSG_ORDER_COMPLETED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_DROPSHIPMENT_ORDER_CONFIRMED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType.CREDIT_CARD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod.REGULAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropshipmentOrderFullyInvoicedIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DropshipmentOrderService dropshipmentOrderService;
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
    private RuntimeService runtimeService;

    @BeforeEach
    public void setup() {
        super.setUp();
        timedPollingService.retry(() -> invoiceNumberCounterRepository.deleteAll());
        timedPollingService.retry(() -> invoiceNumberCounterService.init());
        timedPollingService.retry(() -> salesOrderRepository.deleteAllInBatch());
        timedPollingService.retry(() -> dropshipmentInvoiceRowRepository.deleteAllInBatch());
    }

    @Test
    void testHandleDropShipmentOrderShipmentConfirmed() throws JsonProcessingException {
        var salesOrder = createDropshipmentSalesOrder();
        assertThat(dropshipmentInvoiceRowService.getByOrderNumber(salesOrder.getOrderNumber()).size()).isEqualTo(0);
        var message = createShipmentConfirmedMessage(salesOrder);

        var salesOrder2 = createDropshipmentSalesOrder();
        assertThat(dropshipmentInvoiceRowService.getByOrderNumber(salesOrder2.getOrderNumber()).size()).isEqualTo(0);
        var message2 = createShipmentConfirmedMessage(salesOrder2);

        var processInstance1 = startDropshipmentConfirmedProcess(salesOrder, message);
        var processInstance2 = startDropshipmentConfirmedProcess(salesOrder2, message2);

        starInvoicingProcess();

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance1.getId(), END_MSG_ORDER_COMPLETED.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance2.getId(), END_MSG_ORDER_COMPLETED.getName())));

        Set<String> orderNumbers = new TreeSet<>();
        orderNumbers.add(salesOrder.getOrderNumber());
        orderNumbers.add(salesOrder2.getOrderNumber());
        int i = 1;
        for (String orderNumber: orderNumbers) {
            var updatedSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber).get();
            var invoiceNumber = updatedSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber();
            assertThat(invoiceNumber).hasSize(18);
            assertThat(invoiceNumber).isEqualTo(LocalDateTime.now().getYear()
                    + "-100000000000" + Integer.toString(i));
            i++;
            assertThat(updatedSalesOrder.getInvoiceEvent()).isNotNull();
            assertThat(updatedSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()).isEqualTo(invoiceNumber);
        }
    }

    private DropshipmentShipmentConfirmedMessage createShipmentConfirmedMessage(SalesOrder salesOrder) {
        return DropshipmentShipmentConfirmedMessage.builder()
                .salesOrderNumber(salesOrder.getOrderNumber())
                .items(new ArrayList<>(Set.of(ShipmentItem.builder()
                        .productNumber("sku-1")
                        .parcelNumber("00F8F0LT")
                        .trackingLink("http://abc1")
                        .serviceProviderName("abc1")
                        .build(), ShipmentItem.builder()
                        .productNumber("sku-2")
                        .parcelNumber("00F8F0LT2")
                        .trackingLink("http://abc2")
                        .serviceProviderName("abc2")
                        .build(),
                        ShipmentItem.builder()
                                .productNumber("sku-3")
                                .parcelNumber("00F8F0LT32")
                                .trackingLink("http://abc3")
                                .serviceProviderName("abc3")
                                .build())))
                .build();
    }

    private SalesOrder createDropshipmentSalesOrder() {
        var salesOrder = SalesOrderUtil.createNewSalesOrderV3(false, REGULAR, CREDIT_CARD, NEW);
        ((Order) salesOrder.getOriginalOrder()).getOrderHeader().setOrderFulfillment(DELTICOM.getName());

        salesOrderService.save(salesOrder, Action.ORDER_CREATED);
        return salesOrder;
    }

    private ProcessInstance startDropshipmentConfirmedProcess(SalesOrder salesOrder, DropshipmentShipmentConfirmedMessage message) {
        ProcessInstance processInstance = camundaHelper.createOrderProcess(salesOrder, Messages.ORDER_RECEIVED_ECP);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.checkIfActiveProcessExists(salesOrder.getOrderNumber())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_CREATED.getName())));

        var messageCorrelationResult = bpmUtil.sendMessage(Messages.DROPSHIPMENT_ORDER_CONFIRMED, salesOrder.getOrderNumber(),
                Variables.putValue(IS_DROPSHIPMENT_ORDER_CONFIRMED.getName(), true));

        assertThat(messageCorrelationResult.getExecution().getProcessInstanceId()).isNotBlank();

        assertTrue(timerService.pollWithDefaultTiming(() ->
                    camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_PURCHASE_ORDER_SUCCESSFUL.getName())));

        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(message, messageWrapper);

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), DROPSHIPMENT_ORDER_ROW_SHIPMENT_CONFIRMED_SUB_PROCESS.getName())));

        return processInstance;
    }

    private ProcessInstance starInvoicingProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(INVOICING_PROCESS.getName());

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(),AGGREGATE_INVOICE_DATA.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), INVOICING_CREATE_DROPSHIPMENT_SALES_ORDER_INVOICE.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_INVOICING_PUBLISH_FULLY_INVOICED_DATA.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_INVOICING_DROPSHIPMENT_ORDER_FULLY_INVOICED.getName())));

        assertTrue(timerService.pollWithDefaultTiming(() ->
                camundaHelper.hasPassed(processInstance.getId(), EVENT_THROW_MSG_INVOICING_GENERATE_FULLY_INVOICED_PDF.getName())));

        return processInstance;
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