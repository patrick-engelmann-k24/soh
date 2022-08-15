package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.FeatureFlagConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ArticleItemsDto;
import de.kfzteile24.salesOrderHub.dto.sns.parcelshipped.ParcelShippedMessage;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SUBSEQUENT_ORDER_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_CREATED_IN_SOH;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.RETURN_ORDER_CREATED;
import static java.util.function.Predicate.not;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final SalesOrderReturnService salesOrderReturnService;
    private final CamundaHelper camundaHelper;
    private final SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    private final FeatureFlagConfig featureFlagConfig;
    private final SnsPublishService snsPublishService;
    private final CreditNoteEventMapper creditNoteEventMapper;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final SalesOrderProcessService salesOrderCreateService;
    private final MessageWrapperUtil messageWrapperUtil;
    private final MetricsHelper metricsHelper;
    private final OrderUtil orderUtil;
    private final SleuthHelper sleuthHelper;

    private ObjectMapper objectMapper;

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {
            "${soh.sqs.queue.ecpShopOrders}",
            "${soh.sqs.queue.bcShopOrders}",
            "${soh.sqs.queue.coreShopOrders}"
    }, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerShopOrders(String rawMessage, @Header("SenderId") String senderId,
                                        @Header("ApproximateReceiveCount") Integer receiveCount) {

        var orderMessageWrapper = messageWrapperUtil.create(rawMessage, Order.class);
        var order = orderMessageWrapper.getMessage();

        sleuthHelper.updateTraceId(order.getOrderHeader().getOrderNumber());

        log.info("Received shop order message with order number: {}. Platform: {}. Receive count: {}. Sender ID: {}",
                order.getOrderHeader().getOrderNumber(),
                order.getOrderHeader().getPlatform(),
                receiveCount,
                senderId);

        salesOrderCreateService.handleShopOrdersReceived(orderMessageWrapper);
    }

    /**
     * Consume messages from sqs for event order item shipped
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemShipped}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling ItemShipped message", dispatcher = true)
    public void queueListenerItemShipped(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);
        log.info("Received item shipped  message with order number: {} ", fulfillmentMessage.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.ROW_SHIPPED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item shipped",
                rawMessage,
                RowEvents.ROW_SHIPPED);
    }

    /**
     * Consume messages from sqs for order payment secured
     */
    @SqsListener(value = "${soh.sqs.queue.orderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling OrderPaymentSecured message", dispatcher = true)
    public void queueListenerOrderPaymentSecured(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        CoreDataReaderEvent coreDataReaderEvent = objectMapper.readValue(body, CoreDataReaderEvent.class);

        var orderNumber = coreDataReaderEvent.getOrderNumber();
        log.info("Received order payment secured message with order number: {} ", orderNumber);

        Optional.of(salesOrderService.getOrderByOrderNumber(orderNumber)
                        .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber)))
                .filter(not(salesOrderPaymentSecuredService::hasOrderPaypalPaymentType))
                .ifPresentOrElse(p -> salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured(orderNumber),
                        () -> log.info("Order with order number: {} has paypal payment type. Prevent processing order payment secured message", orderNumber));
    }

    /**
     * Consume messages from sqs for order item transmitted to logistic
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTransmittedToLogistic}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling OrderItemTransmittedToLogistic message", dispatcher = true)
    public void queueListenerOrderItemTransmittedToLogistic(String rawMessage,
        @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);
        log.info("Received order item transmitted to logistic message with order number: {} ", fulfillmentMessage.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.ROW_TRANSMITTED_TO_LOGISTICS,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item transmitted to logistic",
                rawMessage,
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);
    }

    /**
     * Consume messages from sqs for event order item packing started
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemPackingStarted}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling OrderItemPacking message", dispatcher = true)
    public void queueListenerOrderItemPackingStarted(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);
        log.info("Received order item packing message with order number: {} ", fulfillmentMessage.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.PACKING_STARTED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item packing started",
                rawMessage,
                RowEvents.PACKING_STARTED);
    }

    /**
     * Consume messages from sqs for event order item tracking id received
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTrackingIdReceived}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling OrderItemTrackingIdReceived message", dispatcher = true)
    public void queueListenerOrderItemTrackingIdReceived(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);
        log.info("Received order item tracking id message with order number: {} ", fulfillmentMessage.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.TRACKING_ID_RECEIVED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item tracking id received",
                rawMessage,
                RowEvents.TRACKING_ID_RECEIVED);
    }

    /**
     * Consume messages from sqs for event order item tour started
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTourStarted}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling OrderItemTourStarted message", dispatcher = true)
    public void queueListenerOrderItemTourStarted(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);
        log.info("Received order item tour started message with order number: {} ", fulfillmentMessage.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.TOUR_STARTED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item tour started",
                rawMessage,
                RowEvents.TOUR_STARTED);
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
                salesOrderService.handleCreditNoteFromDropshipmentOrderReturn(invoiceUrl);
            } else {
                salesOrderService.handleInvoiceFromCore(invoiceUrl);
            }
        } catch (Exception e) {
            log.error("Invoice received from core message error - invoice url: {}\r\nErrorMessage: {}", invoiceUrl, e);
            throw e;
        }
    }

    /**
     * Consume messages from sqs for event core cancellation
     */
    @Deprecated(since = "We are switching to core sales invoice: queueListenerCoreSalesInvoiceCreated")
    @SqsListener(value = "${soh.sqs.queue.coreCancellation}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling CoreCancellation message", dispatcher = true)
    public void queueListenerCoreCancellation(String rawMessage,
                                              @Header("SenderId") String senderId,
                                              @Header("ApproximateReceiveCount") Integer receiveCount) {
        log.info("Received message on deprecated queue coreCancellation. \nMessage is ignored.");
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
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling d365OrderPaymentSecured message", dispatcher = true)
    public void queueListenerD365OrderPaymentSecured(
        String rawMessage,
        @Header("SenderId") String senderId,
        @Header("ApproximateReceiveCount") Integer receiveCount) {

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        OrderPaymentSecuredMessage orderPaymentSecuredMessage = objectMapper.readValue(body, OrderPaymentSecuredMessage.class);

        var orderNumbers = orderPaymentSecuredMessage.getData().getSalesOrderId().stream()
                .filter(not(dropshipmentOrderService::isDropShipmentOrder))
                .toArray(String[]::new);
        log.info("Received d365 order payment secured message with order group id: {} and order numbers: {}",
            orderPaymentSecuredMessage.getData().getOrderGroupId(),  orderNumbers);

        salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured(orderNumbers);
    }

    /**
     * Consume messages from sqs for dropshipment shipment confirmed published by P&R
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentShipmentConfirmed}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling dropshipment shipment confirmed message", dispatcher = true)
    public void queueListenerDropshipmentShipmentConfirmed(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentShipmentConfirmedMessage shipmentConfirmedMessage =
                objectMapper.readValue(body, DropshipmentShipmentConfirmedMessage.class);

        log.info("Received dropshipment shipment confirmed message with order number: {}",
                shipmentConfirmedMessage.getSalesOrderNumber());

        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(shipmentConfirmedMessage);
    }

    /**
     * Consume messages from sqs for dropshipment purchase order booked
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderBooked}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling Dropshipment Purchase Order Booked message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderBooked(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderBookedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderBookedMessage.class);

        log.info("Received drop shipment order purchased booked message with Sales Order Number: {}, External Order NUmber: {}",
                message.getSalesOrderNumber(), message.getExternalOrderNumber());

        dropshipmentOrderService.handleDropShipmentOrderConfirmed(message);
    }

    /**
     * Consume messages from sqs for dropshipment purchase order return confirmed
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderReturnConfirmed}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling Dropshipment Purchase Order Return Confirmed Message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderReturnConfirmed(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        DropshipmentPurchaseOrderReturnConfirmedMessage message =
                objectMapper.readValue(body, DropshipmentPurchaseOrderReturnConfirmedMessage.class);

        log.info("Received dropshipment purchase order return confirmed message with Sales Order Number: {}, External Order NUmber: {}",
                message.getSalesOrderNumber(), message.getExternalOrderNumber());

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message);
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

        var messageWrapper =
                messageWrapperUtil.create(rawMessage, DropshipmentPurchaseOrderReturnNotifiedMessage.class);

        var message = messageWrapper.getMessage();

        log.info("Received dropshipment purchase order return notified message with " +
                        "Sales Order Number: {}, External Order Number: {}, Sender Id: {}, Received Count {}",
                message.getSalesOrderNumber(), message.getExternalOrderNumber(), senderId, receiveCount);

        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnNotified(messageWrapper);
    }

    /**
     * Consume messages from sqs for core sales credit note created published by core-publisher
     */
    @SqsListener(value = "${soh.sqs.queue.coreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling core sales credit note created message", dispatcher = true)
    public void queueListenerCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        if (featureFlagConfig.getIgnoreCoreCreditNote()) {
            log.info("Core Credit Note is ignored");
        } else {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                    objectMapper.readValue(body, SalesCreditNoteCreatedMessage.class);

            var orderNumber = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
            log.info("Received core sales credit note created message with order number: {}", orderNumber);
            salesOrderRowService.handleSalesOrderReturn(salesCreditNoteCreatedMessage, RETURN_ORDER_CREATED, CORE_CREDIT_NOTE_CREATED);
        }
    }

    /**
     * Consume messages from sqs for core sales invoice created
     */
    @SqsListener(value = "${soh.sqs.queue.coreSalesInvoiceCreated}")
    @SneakyThrows(JsonProcessingException.class)
    @Transactional
    @Trace(metricName = "Handling core sales invoice created message", dispatcher = true)
    public void queueListenerCoreSalesInvoiceCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        if (featureFlagConfig.getIgnoreCoreSalesInvoice()) {
            log.info("Core Sales Invoice is ignored");
        } else {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage = objectMapper.readValue(body, CoreSalesInvoiceCreatedMessage.class);
            CoreSalesInvoiceHeader salesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
            var itemList = salesInvoiceHeader.getInvoiceLines();
            var orderNumber = salesInvoiceHeader.getOrderNumber();
            var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
            var newOrderNumber = salesOrderService.createOrderNumberInSOH(orderNumber, invoiceNumber);
            log.info("Received core sales invoice created message with order number: {} and invoice number: {}",
                    orderNumber, invoiceNumber);

            try {
                // Fetch original sales order
                var originalSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                        .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

                if (!isInvoicePublished(originalSalesOrder, invoiceNumber)
                        && salesOrderService.isFullyMatchedWithOriginalOrder(originalSalesOrder, itemList)) {
                    updateOriginalSalesOrder(salesInvoiceCreatedMessage, originalSalesOrder);
                    publishInvoiceEvent(originalSalesOrder);
                } else {
                    if (salesOrderService.checkOrderNotExists(newOrderNumber)) {
                        SalesOrder subsequentOrder = salesOrderService.createSalesOrderForInvoice(
                                salesInvoiceCreatedMessage,
                                originalSalesOrder,
                                newOrderNumber);
                        handleCancellationForOrderRows(originalSalesOrder, subsequentOrder.getLatestJson().getOrderRows());
                        Order order = subsequentOrder.getLatestJson();
                        if(orderUtil.checkIfOrderHasOrderRows(order)){
                            ProcessInstance result = camundaHelper.createOrderProcess(subsequentOrder, ORDER_CREATED_IN_SOH);
                            if (result != null) {
                                log.info("New soh order process started by core sales invoice created message with " +
                                                "order number: {} and invoice number: {}. Process-Instance-ID: {} ",
                                        orderNumber,
                                        invoiceNumber,
                                        result.getProcessInstanceId());
                                metricsHelper.sendCustomEvent(subsequentOrder, SUBSEQUENT_ORDER_GENERATED);
                            }
                        } else {
                            snsPublishService.publishOrderCreated(subsequentOrder.getOrderNumber());
                        }
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

        var invoiceNumber = invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber();
        originalSalesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceNumber);
        invoiceMsg.getSalesInvoice().getSalesInvoiceHeader().setOrderGroupId(
                originalSalesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        originalSalesOrder.setInvoiceEvent(invoiceMsg);
        salesOrderService.updateOrder(originalSalesOrder);
    }

    protected void handleCancellationForOrderRows(SalesOrder originalSalesOrder, List<OrderRows> orderRows) {

        var originalOrderRowsNotCancelled = originalSalesOrder.getLatestJson().getOrderRows().stream()
                .filter(row -> !row.getIsCancelled()).collect(Collectors.toSet());

        for (OrderRows orderRow : orderRows) {

            var originalSkusToCancel = originalOrderRowsNotCancelled.stream()
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

    /**
     * Consume messages from sqs for migration core sales order created published by core-publisher
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesOrderCreated}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling migration core sales order created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesOrderCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        if (featureFlagConfig.getIgnoreMigrationCoreSalesOrder()) {
            log.info("Migration Core Sales Order is ignored");
        } else {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            Order order = objectMapper.readValue(body, Order.class);
            String orderNumber = order.getOrderHeader().getOrderNumber();

            final Optional<SalesOrder> salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
            if (salesOrder.isPresent()) {
                salesOrderService.updateSalesOrderByOrderJson(salesOrder.get(), order);
                log.info("Order with order number: {} is duplicated for migration. Publishing event on migration topic", orderNumber);
                snsPublishService.publishMigrationOrderCreated(orderNumber);
            } else {
                log.info("Order with order number: {} is a new order. Call redirected to normal flow.", orderNumber);
                MessageWrapper<Order> orderMessageWrapper = messageWrapperUtil.create(rawMessage, Order.class);
                var newSalesOrder = SalesOrder
                        .builder()
                        .orderNumber(order.getOrderHeader().getOrderNumber())
                        .orderGroupId(order.getOrderHeader().getOrderGroupId())
                        .salesChannel(order.getOrderHeader().getSalesChannel())
                        .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
                        .originalOrder(order)
                        .latestJson(order)
                        .build();
                salesOrderCreateService.startSalesOrderProcess(SalesOrderSplit.regularOrder(newSalesOrder), orderMessageWrapper);
            }
        }

    }

    /**
     * Consume messages from sqs for migration core sales invoice created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesInvoiceCreated}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling migration core sales invoice created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesInvoiceCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

        if (featureFlagConfig.getIgnoreMigrationCoreSalesInvoice()) {
            log.info("Migration Core Sales Invoice is ignored");
        } else {
            String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
            CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage = objectMapper.readValue(body, CoreSalesInvoiceCreatedMessage.class);
            CoreSalesInvoiceHeader salesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
            var orderNumber = salesInvoiceHeader.getOrderNumber();
            var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
            log.info("Received migration core sales invoice created message with order number: {} and invoice number: {}",
                    orderNumber, invoiceNumber);

            try {
                var optionalSalesOrder = salesOrderService.getOrderByOrderGroupId(orderNumber).stream()
                        .filter(salesOrder -> invoiceNumber.equals(salesOrder.getLatestJson().getOrderHeader().getDocumentRefNumber()))
                        .findFirst();

                if (optionalSalesOrder.isPresent()) {
                    salesOrderRowService.handleMigrationSubsequentOrder(salesInvoiceCreatedMessage, optionalSalesOrder.get());
                } else {
                    log.info("Invoice with invoice number: {} is a new invoice. Call redirected to normal flow.", invoiceNumber);
                    queueListenerCoreSalesInvoiceCreated(rawMessage, senderId, receiveCount);
                }
            } catch (Exception e) {
                log.error("Migration core sales invoice created received message error:\r\nOrderNumber: {}\r\nInvoiceNumber: {}\r\nError-Message: {}",
                        orderNumber,
                        invoiceNumber,
                        e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Consume messages from sqs for migration core sales credit note created
     */
    @SqsListener(value = "${soh.sqs.queue.migrationCoreSalesCreditNoteCreated}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling migration core sales credit note created message", dispatcher = true)
    @Transactional
    public void queueListenerMigrationCoreSalesCreditNoteCreated(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount) {

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
                log.info("Publishing migration credit note created event for order number {} and credit note number: {}",
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
    }

    /**
     * Consume messages from sqs for event parcel shipped
     * to trigger emails on soh-communication-service for regular orders
     */
    @SqsListener(value = "${soh.sqs.queue.parcelShipped}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling ParcelShipped message", dispatcher = true)
    public void queueListenerParcelShipped(String rawMessage,
                                           @Header("SenderId") String senderId,
                                           @Header("ApproximateReceiveCount") Integer receiveCount) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        var message = objectMapper.readValue(body, ParcelShippedMessage.class);
        var event = message.getMessage();
        var orderNumber = event.getOrderNumber();
        log.info("Parcel shipped received with order number {}, delivery note number {}, " +
                        "tracking link: {} and order items: {}",
                orderNumber,
                event.getDeliveryNoteNumber(),
                event.getTrackingLink(),
                event.getArticleItemsDtos().stream()
                        .map(ArticleItemsDto::getNumber)
                        .collect(Collectors.toList()));

        salesOrderRowService.handleParcelShippedEvent(event);
    }

    @Autowired
    public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
