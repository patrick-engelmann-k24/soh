package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.helper.SalesOrderMapper;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.MIGRATION_SALES_ORDER_RECEIVED;
import static java.text.MessageFormat.format;
import static java.util.function.Predicate.not;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final SalesOrderReturnService salesOrderReturnService;
    private final SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    private final FeatureFlagConfig featureFlagConfig;
    private final SnsPublishService snsPublishService;
    private final CreditNoteEventMapper creditNoteEventMapper;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final SalesOrderProcessService salesOrderCreateService;
    private final MessageWrapperUtil messageWrapperUtil;
    private final CoreSalesInvoiceCreatedService coreSalesInvoiceCreatedService;
    private final CoreSalesCreditNoteCreatedService coreSalesCreditNoteCreatedService;
    private final ParcelShippedService parcelShippedService;
    private final SQSNamesConfig sqsNamesConfig;
    private final SalesOrderMapper salesOrderMapper;
    private final CamundaHelper camundaHelper;
    private final MessageErrorHandler messageErrorHandler;
    private ObjectMapper objectMapper;

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.ecpShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerEcpShopOrders(String rawMessage, @Header("SenderId") String senderId,
                                           @Header("ApproximateReceiveCount") Integer receiveCount) {
        String sqsName = sqsNamesConfig.getEcpShopOrders();
        salesOrderCreateService.handleShopOrdersReceived(rawMessage, receiveCount, sqsName, senderId);
    }

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.bcShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerBcShopOrders(String rawMessage, @Header("SenderId") String senderId,
                                          @Header("ApproximateReceiveCount") Integer receiveCount) {
        String sqsName = sqsNamesConfig.getBcShopOrders();
        salesOrderCreateService.handleShopOrdersReceived(rawMessage, receiveCount, sqsName, senderId);
    }

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.coreShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerCoreShopOrders(String rawMessage, @Header("SenderId") String senderId,
                                            @Header("ApproximateReceiveCount") Integer receiveCount) {
        String sqsName = sqsNamesConfig.getCoreShopOrders();
        salesOrderCreateService.handleShopOrdersReceived(rawMessage, receiveCount, sqsName, senderId);
    }

    /**
     * Consume messages from sqs for order payment secured
     */
    @SqsListener(value = "${soh.sqs.queue.orderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderPaymentSecured message", dispatcher = true)
    public void queueListenerOrderPaymentSecured(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        try {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            CoreDataReaderEvent coreDataReaderEvent = objectMapper.readValue(body, CoreDataReaderEvent.class);

            var orderNumber = coreDataReaderEvent.getOrderNumber();
            log.info("Received order payment secured message with order number: {} ", orderNumber);

            Optional.of(salesOrderService.getOrderByOrderNumber(orderNumber)
                            .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber)))
                    .filter(not(salesOrderPaymentSecuredService::hasOrderPaypalPaymentType))
                    .ifPresentOrElse(p -> salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured(orderNumber),
                            () -> log.info("Order with order number: {} has paypal payment type. Prevent processing order" +
                                    " payment secured message", orderNumber));
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for event invoice from core
     */
    @SqsListener(value = "${soh.sqs.queue.invoicesFromCore}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling InvoiceReceived message", dispatcher = true)
    public void queueListenerInvoiceReceivedFromCore(String rawMessage,
                                                     @Header("SenderId") String senderId,
                                                     @Header("ApproximateReceiveCount") Integer receiveCount) {
        final var invoiceUrl = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();

        try {
            if (InvoiceUrlExtractor.matchesCreditNoteNumberPattern(invoiceUrl)) {
                camundaHelper.handleCreditNoteFromDropshipmentOrderReturn(invoiceUrl);
            } else {
                camundaHelper.handleInvoiceFromCore(invoiceUrl);
            }
        } catch (Exception e) {
            log.error("Invoice received from core message error - invoice url: {}\r\nErrorMessage: {}", invoiceUrl, e);
            throw e;
        }
    }

    /**
     * Consume messages from sqs for subsequent delivery received
     */
    @Deprecated(since = "We are switching to core sales invoice: queueListenerCoreSalesInvoiceCreated")
    @SqsListener(value = "${soh.sqs.queue.subsequentDeliveryReceived}")
    @Trace(metricName = "Handling subsequent delivery note printed message", dispatcher = true)
    public void queueListenerSubsequentDeliveryReceived(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        log.info("Received message on deprecated queue subsequentDeliveryReceived. \nMessage is ignored.");
    }

    /**
     * Consume messages from sqs for order payment secured published by D365
     */
    @SqsListener(value = "${soh.sqs.queue.d365OrderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling d365OrderPaymentSecured message", dispatcher = true)
    public void queueListenerD365OrderPaymentSecured(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        try {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            OrderPaymentSecuredMessage orderPaymentSecuredMessage = objectMapper.readValue(body,
                    OrderPaymentSecuredMessage.class);

            var orderNumbers = orderPaymentSecuredMessage.getData().getSalesOrderId().stream()
                    .filter(not(dropshipmentOrderService::isDropShipmentOrder))
                    .toArray(String[]::new);
            log.info("Received d365 order payment secured message with order group id: {} and order numbers: {}",
                    orderPaymentSecuredMessage.getData().getOrderGroupId(), orderNumbers);

            salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured(orderNumbers);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for dropshipment shipment confirmed published by P&R
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentShipmentConfirmed}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling dropshipment shipment confirmed message", dispatcher = true)
    public void queueListenerDropshipmentShipmentConfirmed(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            DropshipmentShipmentConfirmedMessage shipmentConfirmedMessage =
                    objectMapper.readValue(body, DropshipmentShipmentConfirmedMessage.class);

            log.info("Received dropshipment shipment confirmed message with order number: {}",
                    shipmentConfirmedMessage.getSalesOrderNumber());

            dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(shipmentConfirmedMessage);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for dropshipment purchase order booked
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderBooked}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling Dropshipment Purchase Order Booked message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderBooked(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            DropshipmentPurchaseOrderBookedMessage message =
                    objectMapper.readValue(body, DropshipmentPurchaseOrderBookedMessage.class);

            log.info("Received drop shipment order purchased booked message with Sales Order Number: {}, External Order " +
                            "NUmber: {}",
                    message.getSalesOrderNumber(), message.getExternalOrderNumber());

            dropshipmentOrderService.handleDropShipmentOrderConfirmed(message);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for dropshipment purchase order return confirmed
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderReturnConfirmed}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling Dropshipment Purchase Order Return Confirmed Message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderReturnConfirmed(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            DropshipmentPurchaseOrderReturnConfirmedMessage message =
                    objectMapper.readValue(body, DropshipmentPurchaseOrderReturnConfirmedMessage.class);

            log.info("Received dropshipment purchase order return confirmed message with Sales Order Number: {}, External" +
                            " Order NUmber: {}",
                    message.getSalesOrderNumber(), message.getExternalOrderNumber());

            dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for dropshipment purchase order booked
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderReturnNotified}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling Dropshipment Purchase Order Return Notified message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderReturnNotified(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {
        try {
            var messageWrapper =
                    messageWrapperUtil.create(rawMessage, DropshipmentPurchaseOrderReturnNotifiedMessage.class);

            var message = messageWrapper.getMessage();

            log.info("Received dropshipment purchase order return notified message with " +
                            "Sales Order Number: {}, External Order Number: {}, Sender Id: {}, Received Count {}",
                    message.getSalesOrderNumber(), message.getExternalOrderNumber(), senderId, receiveCount);

            dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnNotified(messageWrapper);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for core sales credit note created published by core-publisher
     */
    @SqsListener(value = "${soh.sqs.queue.coreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows
    @Trace(metricName = "Handling core sales credit note created message", dispatcher = true)
    public void queueListenerCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String sqsName = sqsNamesConfig.getCoreSalesCreditNoteCreated();
        coreSalesCreditNoteCreatedService.handleCoreSalesCreditNoteCreated(rawMessage, receiveCount, sqsName);
    }

    /**
     * Consume messages from sqs for core sales invoice created
     */
    @SneakyThrows
    @Transactional
    @SqsListener(value = "${soh.sqs.queue.coreSalesInvoiceCreated}")
    @Trace(metricName = "Handling core sales invoice created message", dispatcher = true)
    public void queueListenerCoreSalesInvoiceCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String sqsName = sqsNamesConfig.getCoreSalesInvoiceCreated();
        coreSalesInvoiceCreatedService.handleCoreSalesInvoiceCreated(rawMessage, receiveCount, sqsName);
    }

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
                String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
                Order order = objectMapper.readValue(body, Order.class);
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
                String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
                CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage = objectMapper.readValue(body,
                        CoreSalesInvoiceCreatedMessage.class);
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
                        queueListenerCoreSalesInvoiceCreated(rawMessage, senderId, receiveCount);
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
                String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
                var salesCreditNoteCreatedMessage =
                        objectMapper.readValue(body, SalesCreditNoteCreatedMessage.class);
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
                    queueListenerCoreSalesCreditNoteCreated(rawMessage, senderId, receiveCount);
                }
            }
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for event parcel shipped
     * to trigger emails on soh-communication-service for regular orders
     */
    @SqsListener(value = "${soh.sqs.queue.parcelShipped}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling ParcelShipped message", dispatcher = true)
    public void queueListenerParcelShipped(String rawMessage,
                                           @Header("SenderId") String senderId,
                                           @Header("ApproximateReceiveCount") Integer receiveCount) {
        String sqsName = sqsNamesConfig.getParcelShipped();
        parcelShippedService.handleParcelShipped(rawMessage, receiveCount, sqsName);
    }

    @Autowired
    public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
