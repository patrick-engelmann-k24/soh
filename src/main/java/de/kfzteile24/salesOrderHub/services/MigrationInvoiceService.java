package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationInvoiceService {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final OrderUtil orderUtil;

    private final FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;

    @Transactional
    public void handleMigrationSubsequentOrder(CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage,
                                               SalesOrder salesOrder) {
        var coreSalesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        var orderNumber = coreSalesInvoiceHeader.getOrderNumber();
        var invoiceNumber = coreSalesInvoiceHeader.getInvoiceNumber();
        var newOrderNumber = orderUtil.createOrderNumberInSOH(orderNumber, invoiceNumber);

        if (!salesOrder.getOrderNumber().equals(orderNumber)) {
            var originalSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
            handleMigrationCancellationForOrderRows(
                    originalSalesOrder,
                    salesOrder.getLatestJson().getOrderRows(),
                    invoiceNumber);
            snsPublishService.publishMigrationOrderCreated(newOrderNumber);
            log.info("Invoice with order number {} and invoice number: {} is duplicated with the original sales order. " +
                    "Publishing event on migration topic", orderNumber, invoiceNumber);
        }
        snsPublishService.publishCoreInvoiceReceivedEvent(
                EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        log.info("Publishing migration invoice created event with order number {} and invoice number: {}",
                orderNumber,
                invoiceNumber);
    }

    protected void handleMigrationCancellationForOrderRows(SalesOrder salesOrder,
                                                           List<OrderRows> subsequentOrderRows,
                                                           String invoiceNumber) {
        List<OrderRows> salesOrderRowList = salesOrder.getLatestJson().getOrderRows();
        Set<String> skuList = salesOrderRowList.stream().map(OrderRows::getSku).collect(Collectors.toSet());
        subsequentOrderRows.stream()
                .filter(row -> skuList.contains(row.getSku()))
                .forEach(row -> {
                    log.info("Invoice with invoice number: {} is duplicated with the subsequent sales order. " +
                                    "Publishing event on sales order row cancelled migration topic for order number {} " +
                                    "and order row id {}",
                            invoiceNumber,
                            salesOrder.getOrderNumber(),
                            row.getSku());
                    snsPublishService.publishMigrationOrderRowCancelled(salesOrder.getOrderNumber(), row.getSku());
                });
        if (salesOrderRowList.stream().allMatch(OrderRows::getIsCancelled)) {
            log.info("Invoice with invoice number: {} is duplicated with the subsequent sales order. " +
                            "Publishing event on sales order cancelled migration topic for order number {}",
                    invoiceNumber, salesOrder.getOrderNumber());
            snsPublishService.publishMigrationOrderCancelled(salesOrder.getLatestJson());
        }
    }

    @Transactional
    public void handleMigrationCoreSalesInvoiceCreated(CoreSalesInvoiceCreatedMessage message, MessageWrapper messageWrapper) {

        CoreSalesInvoiceHeader salesInvoiceHeader = message.getSalesInvoice().getSalesInvoiceHeader();
        var orderNumber = salesInvoiceHeader.getOrderNumber();
        var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
        log.info("Received migration core sales invoice created message with order number: {} and invoice number:" +
                        " {}",
                orderNumber, invoiceNumber);
        var optionalSalesOrder = salesOrderService.getOrderByOrderGroupId(orderNumber).stream()
                .filter(salesOrder -> invoiceNumber.equals(salesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()))
                .findFirst();

        if (optionalSalesOrder.isPresent()) {
            handleMigrationSubsequentOrder(message, optionalSalesOrder.get());
        } else {
            log.info("Invoice with invoice number: {} is a new invoice. Call redirected to normal flow.",
                    invoiceNumber);
            financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(message, messageWrapper);
        }
    }
}