package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.NotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SUBSEQUENT_ORDER_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_SALES_INVOICE_CREATED_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_GROUP_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PUBLISH_DELAY;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_ORDER_ID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSalesInvoiceService {

    private final FeatureFlagConfig featureFlagConfig;
    private final OrderUtil orderUtil;
    private final CamundaHelper camundaHelper;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final MetricsHelper metricsHelper;
    private final SnsPublishService snsPublishService;

    @Value("${kfzteile.process-config.subsequent-order-process.publish-delay}")
    @Setter
    private String publishDelayForSubsequentOrders;

    @SneakyThrows
    @EnrichMessageForDlq
    public void handleCoreSalesInvoiceCreated(CoreSalesInvoiceCreatedMessage message, MessageWrapper messageWrapper) {

        if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreCoreSalesInvoice())) {
            log.info("Core Sales Invoice is ignored");
        } else {
            CoreSalesInvoiceHeader salesInvoiceHeader = message.getSalesInvoice().getSalesInvoiceHeader();
            var itemList = salesInvoiceHeader.getInvoiceLines();
            var orderNumber = salesInvoiceHeader.getOrderNumber();
            var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
            var newOrderNumber = salesOrderService.createOrderNumberInSOH(orderNumber, invoiceNumber);
            log.info("Received core sales invoice created message with order number: {} and invoice number: {}", orderNumber, invoiceNumber);

            try {
                var originalSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                        .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

                boolean invoicePublished = isInvoicePublished(originalSalesOrder, invoiceNumber);
                if (!invoicePublished && salesOrderService.isFullyMatchedWithOriginalOrder(originalSalesOrder, itemList)) {
                    updateOriginalSalesOrder(message, originalSalesOrder);
                    publishInvoiceEvent(originalSalesOrder);
                    sendInvoiceCreatedMessage(originalSalesOrder);
                } else {
                    if (salesOrderService.checkOrderNotExists(newOrderNumber)) {
                        SalesOrder subsequentOrder = salesOrderService.createSalesOrderForInvoice(
                                message,
                                originalSalesOrder,
                                newOrderNumber);
                        if (!invoicePublished) {
                            salesOrderRowService.handleCancellationForOrderRows(originalSalesOrder.getOrderNumber(), subsequentOrder.getLatestJson().getOrderRows());
                        }
                        if (orderUtil.checkIfOrderHasOrderRows(subsequentOrder.getLatestJson())) {
                            metricsHelper.sendCustomEvent(subsequentOrder, SUBSEQUENT_ORDER_GENERATED);
                        }
                        snsPublishService.publishOrderCreated(subsequentOrder.getOrderNumber());
                        publishInvoiceEvent(subsequentOrder);
                    }
                    if (isOrderCancelled(originalSalesOrder.getOrderNumber())) {
                        sendInvoiceCreatedMessage(originalSalesOrder);
                    }
                }
            } catch (Exception e) {
                log.error("Core sales invoice created received message error:\r\nOrderNumber: {}\r\nInvoiceNumber: {}\r\nError-Message: {}",
                        orderNumber,
                        invoiceNumber,
                        e.getMessage());
                throw e;
            }
        }
    }

    private boolean isOrderCancelled(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        return salesOrder.isCancelled();
    }

    private void sendInvoiceCreatedMessage(SalesOrder originalSalesOrder) {
        var processInstanceId = originalSalesOrder.getProcessId();
        if (Objects.nonNull(processInstanceId) && camundaHelper.waitsOnActivityForMessage(originalSalesOrder.getProcessId(),
                MSG_ORDER_CORE_SALES_INVOICE_CREATED, ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED)) {
            camundaHelper.correlateMessage(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED, originalSalesOrder);
        }
    }

    @EnrichMessageForDlq
    public void handleInvoiceFromCore(String invoiceUrl, MessageWrapper messageWrapper) {
        final var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);

        log.info("Received invoice from core with order number: {} ", orderNumber);

        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), orderNumber,
                INVOICE_URL.getName(), invoiceUrl
        );
        camundaHelper.startProcessByMessage(Messages.INVOICE_CREATED, orderNumber, processVariables);
        log.info("Invoice {} from core for order-number {} successfully received", invoiceUrl, orderNumber);
    }

    public ProcessInstance startInvoiceCreatedReceivedProcess(SalesOrder salesOrder) {
        if (salesOrder.getId() == null)
            throw new NotFoundException("Sales order id could not be null");

        Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), salesOrder.getOrderNumber(),
                ORDER_GROUP_ID.getName(), salesOrder.getOrderGroupId(),
                SALES_ORDER_ID.getName(), salesOrder.getId(),
                PUBLISH_DELAY.getName(), publishDelayForSubsequentOrders);

        return camundaHelper.correlateMessage(CORE_SALES_INVOICE_CREATED_RECEIVED,
                        salesOrder.getId().toString(), processVariables).getProcessInstance();
    }

    private boolean isInvoicePublished(SalesOrder originalSalesOrder, String invoiceNumber) {
        if (originalSalesOrder.getInvoiceEvent() != null
                && Objects.equals(invoiceNumber,
                originalSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber())) {
            throw new IllegalArgumentException(String.format("New Sales Invoice Created Event has the same invoice number as the previous " +
                    "Sales Invoice Created Event: %s", originalSalesOrder.getInvoiceEvent().getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber()));
        }
        return StringUtils.isNotBlank(originalSalesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber())
                && originalSalesOrder.getInvoiceEvent() != null;
    }

    private void publishInvoiceEvent(SalesOrder salesOrder) {

        ProcessInstance result = startInvoiceCreatedReceivedProcess(salesOrder);

        if (result != null) {
            log.info("Order process for publishing core sales invoice created msg is started with " +
                            "order number: {} and sales order id: {}. Process-Instance-ID: {} ",
                    salesOrder.getOrderNumber(),
                    salesOrder.getId(),
                    result.getProcessInstanceId());
        }
    }

    protected void updateOriginalSalesOrder(CoreSalesInvoiceCreatedMessage invoiceMsg,
                                            SalesOrder originalSalesOrder) {

        var invoiceNumber = invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber();
        originalSalesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceNumber);
        invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().setOrderGroupId(
                originalSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        originalSalesOrder.setInvoiceEvent(invoiceMsg);
        salesOrderService.updateOrder(originalSalesOrder);
    }
}
