package de.kfzteile24.salesOrderHub.services.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.FinancialDocumentsSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static java.text.MessageFormat.format;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationSqsReceiveService {

    private final FinancialDocumentsSqsReceiveService financialDocumentsSqsReceiveService;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final SalesOrderReturnService salesOrderReturnService;
    private final FeatureFlagConfig featureFlagConfig;
    private final SnsPublishService snsPublishService;
    private final CreditNoteEventMapper creditNoteEventMapper;
    private final MessageWrapperUtil messageWrapperUtil;
    private final SalesOrderMapper salesOrderMapper;
    private final MessageErrorHandler messageErrorHandler;
    private ObjectMapper objectMapper;

    /**
     * Consume messages from sqs for migration core sales order created published by core-publisher
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesOrderCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales order created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesOrderCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            if (Boolean.TRUE.equals(featureFlagConfig.getIgnoreMigrationCoreSalesOrder())) {
                log.info("Migration Core Sales Order is ignored");
            } else {
                var messageWrapper = messageWrapperUtil.create(rawMessage, Order.class);
                var order = messageWrapper.getMessage();
                String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
                Order originalOrder = objectMapper.readValue(body, Order.class);
                String orderNumber = order.getOrderHeader().getOrderNumber();

                salesOrderService.getOrderByOrderNumber(orderNumber)
                        .ifPresentOrElse(salesOrder -> {
                            salesOrderService.enrichSalesOrder(salesOrder, order, originalOrder);
                            salesOrderService.save(salesOrder, MIGRATION_SALES_ORDER_RECEIVED);
                            log.info("Order with order number: {} is duplicated for migration. Publishing event on " +
                                    "migration topic", orderNumber);
                        }, () -> {
                            var salesOrder = salesOrderMapper.map(order);
                            salesOrderService.enrichSalesOrder(salesOrder, order, originalOrder);
                            log.info("Order with order number: {} is a new migration order. No process will be created.",
                                    orderNumber);
                            salesOrderService.createSalesOrder(salesOrder);
                        });

                snsPublishService.publishMigrationOrderCreated(orderNumber);
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for migration core sales invoice created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesInvoiceCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales invoice created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesInvoiceCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            if (featureFlagConfig.getIgnoreMigrationCoreSalesInvoice()) {
                log.info("Migration Core Sales Invoice is ignored");
            } else {
                var messageWrapper =
                        messageWrapperUtil.create(rawMessage, CoreSalesInvoiceCreatedMessage.class);
                var salesInvoiceCreatedMessage = messageWrapper.getMessage();
                CoreSalesInvoiceHeader salesInvoiceHeader =
                        salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
                var orderNumber = salesInvoiceHeader.getOrderNumber();
                var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
                log.info("Received migration core sales invoice created message with order number: {} and invoice number:" +
                                " {}",
                        orderNumber, invoiceNumber);

                try {
                    var optionalSalesOrder = salesOrderService.getOrderByOrderGroupId(orderNumber).stream()
                            .filter(salesOrder -> invoiceNumber.equals(salesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()))
                            .findFirst();

                    if (optionalSalesOrder.isPresent()) {
                        salesOrderRowService.handleMigrationSubsequentOrder(salesInvoiceCreatedMessage,
                                optionalSalesOrder.get());
                    } else {
                        log.info("Invoice with invoice number: {} is a new invoice. Call redirected to normal flow.",
                                invoiceNumber);
                        financialDocumentsSqsReceiveService.queueListenerCoreSalesInvoiceCreated(rawMessage, senderId, receiveCount);
                    }
                } catch (Exception e) {
                    messageErrorHandler.logErrorMessage(
                            format("Migration core sales invoice created received message error:\r\nOrderNumber: {0} \r\nInvoiceNumber: {1}", orderNumber, invoiceNumber), e);
                }
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for migration core sales credit note created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling migration core sales credit note created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            if (featureFlagConfig.getIgnoreMigrationCoreSalesCreditNote()) {
                log.info("Migration Core Sales Credit Note is ignored");
            } else {
                var messageWrapper =
                        messageWrapperUtil.create(rawMessage, SalesCreditNoteCreatedMessage.class);
                var salesCreditNoteCreatedMessage = messageWrapper.getMessage();
                var salesCreditNoteHeader = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader();
                var orderNumber = salesCreditNoteHeader.getOrderNumber();
                var creditNoteNumber = salesCreditNoteHeader.getCreditNoteNumber();

                var returnOrder = salesOrderReturnService.getByOrderNumber(
                        salesOrderService.createOrderNumberInSOH(orderNumber, creditNoteNumber));
                if (returnOrder != null) {
                    snsPublishService.publishMigrationReturnOrderCreatedEvent(returnOrder);
                    log.info("Return order with order number {} and credit note number: {} is duplicated for migration. " +
                                    "Publishing event on migration topic",
                            orderNumber,
                            creditNoteNumber);

                    var salesCreditNoteReceivedEvent =
                            creditNoteEventMapper.toSalesCreditNoteReceivedEvent(salesCreditNoteCreatedMessage);
                    snsPublishService.publishCreditNoteReceivedEvent(salesCreditNoteReceivedEvent);
                    log.info("Publishing migration credit note created event for order number {} and credit note number: " +
                                    "{}",
                            orderNumber,
                            creditNoteNumber);
                } else {
                    log.info("Return order with order number {} and credit note number: {} is a new order." +
                                    "Call redirected to normal flow.",
                            orderNumber,
                            creditNoteNumber);
                    financialDocumentsSqsReceiveService.queueListenerCoreSalesCreditNoteCreated(rawMessage, senderId, receiveCount);
                }
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    @Autowired
    public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
