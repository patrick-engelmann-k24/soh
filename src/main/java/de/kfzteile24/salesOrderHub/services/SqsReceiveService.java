package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.dto.sns.CoreCancellationMessage;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.ShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.SubsequentDeliveryMessage;
import de.kfzteile24.salesOrderHub.dto.sns.shipment.ShipmentItem;
import de.kfzteile24.salesOrderHub.dto.sns.cancellation.CoreCancellationItem;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.soh.order.dto.Order;
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
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_PURCHASE_ORDER_BOOKED;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.ORDER_ITEM_SHIPPED;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toUnmodifiableSet;
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
    private final SnsPublishService snsPublishService;

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
    @SqsListener(value = "${soh.sqs.queue.coreCancellation}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling CoreCancellation message", dispatcher = true)
    public void queueListenerCoreCancellation(String rawMessage,
                                              @Header("SenderId") String senderId,
                                              @Header("ApproximateReceiveCount") Integer receiveCount) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();

        CoreCancellationMessage coreCancellationMessage = objectMapper.readValue(body, CoreCancellationMessage.class);

        try {
            final var orderNumber = coreCancellationMessage.getOrderNumber();
            log.info("Received core cancellation with order number: {}, original delivery note: {} and cancellation delivery note: {}",
                    orderNumber,
                    coreCancellationMessage.getOriginalDeliveryNoteNumber(),
                    coreCancellationMessage.getCancellationDeliveryNoteNumber());

            List<String> skuList = coreCancellationMessage.getItems().stream()
                    .map(CoreCancellationItem::getSku).collect(Collectors.toList());
            salesOrderRowService.cancelOrderRows(coreCancellationMessage.getOrderNumber(), skuList);

            log.info("Core cancellation for order number {}, original delivery note: {} and cancellation delivery note: {}",
                    orderNumber,
                    coreCancellationMessage.getOriginalDeliveryNoteNumber(),
                    coreCancellationMessage.getCancellationDeliveryNoteNumber());
        } catch (Exception e) {
            log.error("Core cancellation for order number: {} message error: ", coreCancellationMessage.getOrderNumber(), e);
            throw e;
        }
    }

    /**
     * Consume messages from sqs for subsequent delivery received
     */
    @SqsListener(value = "${soh.sqs.queue.subsequentDeliveryReceived}")
    @SneakyThrows(JsonProcessingException.class)
    @Transactional
    @Trace(metricName = "Handling subsequent delivery note printed message", dispatcher = true)
    public void queueListenerSubsequentDeliveryReceived(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        SubsequentDeliveryMessage subsequent = objectMapper.readValue(body, SubsequentDeliveryMessage.class);
        String newOrderNumber = subsequent.getOrderNumber() + "-" + subsequent.getSubsequentDeliveryNoteNumber();
        log.info("Received subsequent delivery note message with order number: {} ", subsequent.getOrderNumber());

        try {
            SalesOrder salesOrder = salesOrderService.createSalesOrderForSubsequentDelivery(subsequent, newOrderNumber);
            ProcessInstance result = camundaHelper.createOrderProcess(salesOrder, ORDER_CREATED_IN_SOH);

            if (result != null) {
                log.info("New soh order process started for subsequent delivery note with " +
                                "order number: {}. Process-Instance-ID: {} ",
                        newOrderNumber,
                        result.getProcessInstanceId());
            }
        } catch (Exception e) {
            log.error("Subsequent delivery received message error:\r\nOrderNumber: {}\r\nError-Message: {}",
                    newOrderNumber,
                    e.getMessage());
            throw e;
        }
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
        ShipmentConfirmedMessage shipmentConfirmedMessage = objectMapper.readValue(body, ShipmentConfirmedMessage.class);
        var orderNumber = shipmentConfirmedMessage.getSalesOrderNumber();
        log.info("Received dropshipment shipment confirmed message with order number: {}", orderNumber);

        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        var orderRows = salesOrder.getLatestJson().getOrderRows();
        var shippedItems = shipmentConfirmedMessage.getItems();

        shippedItems.forEach(item ->
                    orderRows.stream()
                            .filter(row -> StringUtils.pathEquals(row.getSku(), item.getProductNumber()))
                            .findFirst()
                            .ifPresent(row -> {
                                var parcelNumber = item.getParcelNumber();
                                Optional.ofNullable(row.getTrackingNumbers())
                                        .ifPresentOrElse(trackingNumbers -> trackingNumbers.add(parcelNumber),
                                                () -> row.setTrackingNumbers(List.of(parcelNumber)));
                            })
        );

        var persistedSalesOrder = salesOrderService.save(salesOrder, ORDER_ITEM_SHIPPED);

        var trackingLinks = shippedItems.stream()
                .map(ShipmentItem::getTrackingLink)
                .collect(toUnmodifiableSet());

        snsPublishService.publishSalesOrderShipmentConfirmedEvent(persistedSalesOrder, trackingLinks);
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

        log.info("Received dropshipment order purchased booked message with purchase order number : {}",
                message.getPurchaseOrderNumber());

        salesOrderRowService.handleDropshipmentPurchaseOrderBooked(message);
    }
}
