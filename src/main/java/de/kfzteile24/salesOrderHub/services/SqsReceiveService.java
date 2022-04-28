package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesInvoiceCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SalesCreditNoteCreatedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.invoice.CoreSalesInvoiceHeader;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import de.kfzteile24.soh.order.dto.Platform;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_CREATED_IN_SOH;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static java.util.function.Predicate.not;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    private final RuntimeService runtimeService;
    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final CamundaHelper camundaHelper;
    private final ObjectMapper objectMapper;
    private final SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;

    /**
     * Consume sqs for new orders from ecp shop
     */
    @SqsListener({
            "${soh.sqs.queue.ecpShopOrders}",
            "${soh.sqs.queue.bcShopOrders}",
            "${soh.sqs.queue.coreShopOrders}"
    })
    @SneakyThrows(JsonProcessingException.class)
    @Transactional
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerEcpShopOrders(String rawMessage, @Header("SenderId") String senderId,
        @Header("ApproximateReceiveCount") Integer receiveCount) {

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        SalesOrder salesOrder;
        Order order = objectMapper.readValue(body, Order.class);

        try {
            String orderNumber = order.getOrderHeader().getOrderNumber();
            if (StringUtils.isEmpty(order.getOrderHeader().getOrderGroupId())) {
                order.getOrderHeader().setOrderGroupId(orderNumber);
            }
            salesOrder = SalesOrder.builder()
                    .orderNumber(order.getOrderHeader().getOrderNumber())
                    .orderGroupId(order.getOrderHeader().getOrderGroupId())
                    .salesChannel(order.getOrderHeader().getSalesChannel())
                    .customerEmail(order.getOrderHeader().getCustomer().getCustomerEmail())
                    .originalOrder(order)
                    .latestJson(order)
                    .build();

            log.info("Received message from ecp shop with sender id : {}, order number: {}, Platform: {} ", senderId, order.getOrderHeader().getOrderNumber(), order.getOrderHeader().getPlatform());

            //This condition is introduced temporarily because the self pick-up items created in BC are coming back from core orders which raises duplicate issue.
            if(Platform.CORE.equals(order.getOrderHeader().getPlatform())
                &&  salesOrderService.getOrderByOrderNumber(order.getOrderHeader().getOrderNumber()).isPresent()) {
                    log.error("The following order won't be processed because it exists in SOH system already from another source. " +
                            "Platform: {}, Order Number: {}", order.getOrderHeader().getPlatform(), order.getOrderHeader().getOrderNumber());
                    return;
            }

            ProcessInstance result = camundaHelper.createOrderProcess(
                    salesOrderService.createSalesOrder(salesOrder), ORDER_RECEIVED_ECP);

            if (result != null) {
                log.info("New ecp order process started for order number: {}. Process-Instance-ID: {} ", order.getOrderHeader().getOrderNumber(), result.getProcessInstanceId());
            }
        } catch (Exception e) {
            log.error("New ecp order process is failed by message error:\r\nError-Message: {}, Message Body: {}", e.getMessage(), body);
            throw e;
        }
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

        salesOrderRowService.publishOrderRowMsg(
                RowMessages.ROW_SHIPPED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item shipped",
                rawMessage);
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

        salesOrderRowService.publishOrderRowMsg(
                RowMessages.ROW_TRANSMITTED_TO_LOGISTICS,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item transmitted to logistic",
                rawMessage);
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

        salesOrderRowService.publishOrderRowMsg(
                RowMessages.PACKING_STARTED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item packing started",
                rawMessage);
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

        salesOrderRowService.publishOrderRowMsg(
                RowMessages.TRACKING_ID_RECEIVED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item tracking id received",
                rawMessage);
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

        salesOrderRowService.publishOrderRowMsg(
                RowMessages.TOUR_STARTED,
                fulfillmentMessage.getOrderNumber(),
                fulfillmentMessage.getOrderItemSku(),
                "Order item tour started",
                rawMessage);
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
            final var orderNumber = InvoiceUrlExtractor.extractOrderNumber(invoiceUrl);

            log.info("Received invoice from core with order number: {} ", orderNumber);

            final Map<String, Object> processVariables = Map.of(
                    ORDER_NUMBER.getName(), orderNumber,
                    INVOICE_URL.getName(), invoiceUrl
            );

            runtimeService.startProcessInstanceByMessage(Messages.INVOICE_CREATED.getName(), orderNumber, processVariables);
            log.info("Invoice {} from core for order-number {} successfully received", invoiceUrl, orderNumber);
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

        var orderNumbers = orderPaymentSecuredMessage.getData().getSalesOrderId().toArray(new String[]{});
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

        salesOrderRowService.handleDropshipmentShipmentConfirmed(shipmentConfirmedMessage);
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

        salesOrderRowService.handleDropshipmentPurchaseOrderBooked(message);
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

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        SalesCreditNoteCreatedMessage salesCreditNoteCreatedMessage =
                objectMapper.readValue(body, SalesCreditNoteCreatedMessage.class);

        var orderNumber = salesCreditNoteCreatedMessage.getSalesCreditNote().getSalesCreditNoteHeader().getOrderNumber();
        log.info("Received core sales credit note created message with order number: {}", orderNumber);
        salesOrderRowService.handleSalesOrderReturn(orderNumber, salesCreditNoteCreatedMessage);
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
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        CoreSalesInvoiceCreatedMessage salesInvoiceCreatedMessage = objectMapper.readValue(body, CoreSalesInvoiceCreatedMessage.class);
        CoreSalesInvoiceHeader salesInvoiceHeader = salesInvoiceCreatedMessage.getSalesInvoice().getSalesInvoiceHeader();
        var itemList = salesInvoiceHeader.getInvoiceLines();
        var orderNumber = salesInvoiceHeader.getOrderNumber();
        var invoiceNumber = salesInvoiceHeader.getInvoiceNumber();
        var newOrderNumber = orderNumber + "-" + invoiceNumber;
        log.info("Received core sales invoice created message with order number: {} and invoice number: {}",
                orderNumber, invoiceNumber);

        try {
            // Fetch original sales order
            var originalSalesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

            if (salesOrderService.isFullyMatchedWithOriginalOrder(originalSalesOrder, itemList)) {
                updateOriginalSalesOrder(orderNumber, invoiceNumber, originalSalesOrder);
            } else {
                SalesOrder subsequentOrder = salesOrderService.createSalesOrderForInvoice(
                        salesInvoiceCreatedMessage,
                        originalSalesOrder,
                        newOrderNumber);
                handleCancellationForOrderRows(originalSalesOrder, subsequentOrder.getLatestJson().getOrderRows());
                ProcessInstance result = camundaHelper.createOrderProcess(subsequentOrder, ORDER_CREATED_IN_SOH);
                if (result != null) {
                    log.info("New soh order process started by core sales invoice created message with " +
                                    "order number: {} and invoice number: {}. Process-Instance-ID: {} ",
                            orderNumber,
                            invoiceNumber,
                            result.getProcessInstanceId());
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

    protected void updateOriginalSalesOrder(String orderNumber, String invoiceNumber, SalesOrder originalSalesOrder) {
        originalSalesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceNumber);
        salesOrderService.updateOrder(originalSalesOrder);

        if (!camundaHelper.checkIfActiveProcessExists(orderNumber)) {
            ProcessInstance result = camundaHelper.createOrderProcess(
                    salesOrderService.createSalesOrder(originalSalesOrder), ORDER_RECEIVED_ECP);
            log.info("Original sales order is updated by core sales invoice created message with " +
                            "order number: {} and invoice number: {}.",
                    orderNumber,
                    invoiceNumber);

            if (result != null) {
                log.info("Order process re-started by core sales invoice created message with " +
                                "order number: {} and invoice number: {}. Process-Instance-ID: {} ",
                        orderNumber,
                        invoiceNumber,
                        result.getProcessInstanceId());
            }
        }
    }

    protected void handleCancellationForOrderRows(SalesOrder salesOrder, List<OrderRows> orderRows) {
        var originalOrderRowSkuList = salesOrder.getLatestJson().getOrderRows().stream()
                .filter(row -> !row.getIsCancelled())
                .map(OrderRows::getSku).collect(Collectors.toSet());
        orderRows.stream()
                .filter(row -> originalOrderRowSkuList.contains(row.getSku()))
                .forEach(row -> salesOrderRowService.cancelOrderRow(row.getSku(), salesOrder.getOrderNumber()));
    }
}
