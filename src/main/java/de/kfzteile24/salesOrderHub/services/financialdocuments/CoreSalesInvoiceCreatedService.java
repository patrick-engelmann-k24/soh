package de.kfzteile24.salesOrderHub.services.financialdocuments;

import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.InvoiceHelper;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SUBSEQUENT_ORDER_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events.MSG_ORDER_CORE_SALES_INVOICE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoreSalesInvoiceCreatedService {

    private final FeatureFlagConfig featureFlagConfig;
    private final OrderUtil orderUtil;
    private final CamundaHelper camundaHelper;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final MetricsHelper metricsHelper;
    private final SnsPublishService snsPublishService;
    private final InvoiceHelper invoiceHelper;

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

                var processInstanceId = originalSalesOrder.getProcessId();

                if (Objects.nonNull(processInstanceId) && camundaHelper.waitsOnActivityForMessage(originalSalesOrder.getProcessId(),
                        MSG_ORDER_CORE_SALES_INVOICE_CREATED, ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED)) {
                    camundaHelper.correlateMessage(ORDER_RECEIVED_CORE_SALES_INVOICE_CREATED, originalSalesOrder);
                }

                boolean invoicePublished = isInvoicePublished(originalSalesOrder, invoiceNumber);
                if (!invoicePublished && salesOrderService.isFullyMatchedWithOriginalOrder(originalSalesOrder, itemList)) {
                    updateOriginalSalesOrder(message, originalSalesOrder);
                    publishInvoiceEvent(originalSalesOrder);
                } else {
                    if (salesOrderService.checkOrderNotExists(newOrderNumber)) {
                        SalesOrder subsequentOrder = salesOrderService.createSalesOrderForInvoice(
                                message,
                                originalSalesOrder,
                                newOrderNumber);
                        if (!invoicePublished) {
                            handleCancellationForOrderRows(originalSalesOrder, subsequentOrder.getLatestJson().getOrderRows());
                        }
                        if (orderUtil.checkIfOrderHasOrderRows(subsequentOrder.getLatestJson())) {
                            metricsHelper.sendCustomEvent(subsequentOrder, SUBSEQUENT_ORDER_GENERATED);
                        }
                        snsPublishService.publishOrderCreated(subsequentOrder.getOrderNumber());
                        publishInvoiceEvent(subsequentOrder);
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

        ProcessInstance result = camundaHelper.startInvoiceCreatedReceivedProcess(salesOrder);

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
        invoiceHelper.setSalesOrderInvoiceFromMessage(invoiceMsg, originalSalesOrder);
        salesOrderService.updateOrder(originalSalesOrder);
    }

    protected void handleCancellationForOrderRows(SalesOrder originalSalesOrder, List<OrderRows> orderRows) {

        var originalOrderRowsNotCancelled = originalSalesOrder.getLatestJson().getOrderRows().stream()
                .filter(not(OrderRows::getIsCancelled))
                .collect(toSet());

        for (OrderRows orderRow : orderRows) {

            var originalSkusToCancel = originalOrderRowsNotCancelled.stream()
                    .filter(not(OrderRows::getIsCancelled))
                    .filter(row -> row.getSku().equals(orderRow.getSku())).collect(Collectors.toList());

            if (!originalSkusToCancel.isEmpty()) {
                BigDecimal sumQuantity = originalSkusToCancel.stream().map(OrderRows::getQuantity)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (orderRow.getQuantity().equals(sumQuantity)) {
                    originalSkusToCancel.forEach(row ->
                            salesOrderRowService.cancelOrderRow(row.getSku(), originalSalesOrder.getOrderNumber()));
                } else {
                    salesOrderRowService.cancelOrderRow(originalSkusToCancel.get(0).getSku(), originalSalesOrder.getOrderNumber());
                }
            }

        }
    }
}
