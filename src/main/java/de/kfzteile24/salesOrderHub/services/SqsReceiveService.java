package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.converter.OrderJsonConverter;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.converter.OrderJsonVersionDetector;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.soh.order.dto.Order;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    @NonNull private final RuntimeService runtimeService;
    @NonNull private final SalesOrderService salesOrderService;
    @NonNull private final CamundaHelper camundaHelper;
    @NonNull private final ObjectMapper objectMapper;
    @NonNull private final OrderJsonConverter orderJsonConverter;
    @NonNull private final OrderJsonVersionDetector orderJsonVersionDetector;

    /**
     * Consume sqs for new orders from ecp shop
     */
    @SqsListener("${soh.sqs.queue.ecpShopOrders}")
    @SneakyThrows(JsonProcessingException.class)
    @Transactional
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerEcpShopOrders(String rawMessage, @Header("SenderId") String senderId) {
        log.info("Received message from ecp shop with sender id : {} ", senderId);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        SalesOrder salesOrder;
        if (orderJsonVersionDetector.isVersion2(body)) {
            OrderJSON orderJSON = objectMapper.readValue(body, OrderJSON.class);
            salesOrder = SalesOrder.builder()
                    .orderNumber(orderJSON.getOrderHeader()
                            .getOrderNumber())
                    .orderGroupId(orderJSON.getOrderHeader()
                            .getOrderNumber())//For V2.1 there is no group ID provided, so it will come always null.
                    .salesChannel(orderJSON.getOrderHeader()
                            .getOrigin()
                            .getSalesChannel())
                    .customerEmail(orderJSON.getOrderHeader()
                            .getCustomer()
                            .getCustomerEmail())
                    .originalOrder(orderJSON)
                    .latestJson(orderJsonConverter.convert(orderJSON))
                    .build();
        } else {
            Order order = objectMapper.readValue(body, Order.class);
            String orderNumber = order.getOrderHeader().getOrderNumber();
            if(StringUtils.isEmpty(order.getOrderHeader().getOrderGroupId())){
                order.getOrderHeader().setOrderGroupId(orderNumber);
            }
            salesOrder = SalesOrder.builder()
                    .orderNumber(order.getOrderHeader()
                            .getOrderNumber())
                    .orderGroupId(order.getOrderHeader()
                            .getOrderGroupId())
                    .salesChannel(order.getOrderHeader()
                            .getSalesChannel())
                    .customerEmail(order.getOrderHeader()
                            .getCustomer()
                            .getCustomerEmail())
                    .originalOrder(order)
                    .latestJson(order)
                    .build();
        }

        ProcessInstance result = camundaHelper.createOrderProcess(
                salesOrderService.createSalesOrder(salesOrder), ORDER_RECEIVED_ECP);

        if (result != null) {
            log.info("New ecp order process started. Process-Instance-ID: {} ", result.getProcessInstanceId());
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
        logReceivedMessage(rawMessage, senderId, receiveCount);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.ROW_SHIPPED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order item shipped message for order number {} successfully received", fulfillmentMessage.getOrderNumber());
            }
        } catch (Exception e) {
            log.error("Order item shipped message error:\r\nOrderNumber: {}\r\nOrderItem-SKU: {}\r\nError-Message: {}",
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    e.getMessage()
            );
            throw e;
        }

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
        logReceivedMessage(rawMessage, senderId, receiveCount);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        CoreDataReaderEvent coreDataReaderEvent = objectMapper.readValue(body, CoreDataReaderEvent.class);

        try {
            MessageCorrelationResult result = runtimeService
                    .createMessageCorrelation(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName())
                    .processInstanceBusinessKey(coreDataReaderEvent.getOrderNumber())
                    .correlateWithResult();

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order payment secured message for oder number " + coreDataReaderEvent.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order payment secured message error:\r\nOrderNumber: {}\r\nError-Message: {}",
                    coreDataReaderEvent.getOrderNumber(),
                    e.getMessage()
            );
            throw e;
        }

    }

    /**
     * Consume messages from sqs for order item transmitted to logistic
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTransmittedToLogistic}", deletionPolicy = ON_SUCCESS)
    @SneakyThrows(JsonProcessingException.class)
    @Trace(metricName = "Handling OrderItemTransmittedToLogistic message", dispatcher = true)
    public void queueListenerOrderItemTransmittedToLogistic(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.ROW_TRANSMITTED_TO_LOGISTICS,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order item transmitted to logistic message for order-number {} and sku {} successfully received",
                        fulfillmentMessage.getOrderNumber(),
                        fulfillmentMessage.getOrderItemSku()
                );
            }
        } catch (Exception e) {
            log.error("Order item transmitted to logistic message error: \r\nOrderNumber: {}\r\nOrderItem-SKU: {}\r\nSQS-Message: {}\r\nError-Message: {}",
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    rawMessage,
                    e.getMessage()
            );
            throw e;
        }
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
        logReceivedMessage(rawMessage, senderId, receiveCount);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.PACKING_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order item packing started message for oder-number {} and sku {} successfully received",
                        fulfillmentMessage.getOrderNumber(),
                        fulfillmentMessage.getOrderItemSku()
                );
            }
        } catch (Exception e) {
            log.error("Order item packing started message error:\r\nOrderNumber: {}\r\nOrderItem-SKU: {}\r\nSQS-Message: {}\r\nError-Message: {}",
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    rawMessage,
                    e.getMessage());
            throw e;
        }
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
        logReceivedMessage(rawMessage, senderId, receiveCount);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage = objectMapper.readValue(body, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.TRACKING_ID_RECEIVED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
                    );

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order item tracking id received message for order-number {} and sku {} successfully received",
                        fulfillmentMessage.getOrderNumber(),
                        fulfillmentMessage.getOrderItemSku()
                );
            }
        } catch (Exception e) {
            log.error("Order item tracking id received message error:\r\nOrderNumber: {}\r\nOrderItem-SKU: {}\r\nSQS-Message: {}\r\nError-Message: {}\r\n",
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    rawMessage,
                    e.getMessage()
            );
            throw e;
        }
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
        logReceivedMessage(rawMessage, senderId, receiveCount);

        String body = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();
        FulfillmentMessage fulfillmentMessage =objectMapper.readValue(body, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.TOUR_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (!result.getExecution().getProcessInstanceId().isEmpty()) {
                log.info("Order item tour started message for order-number {} and sku {} successfully received",
                        fulfillmentMessage.getOrderNumber(),
                        fulfillmentMessage.getOrderItemSku()
                );
            }
        } catch (Exception e) {
            log.error("Order item tour started message error:\r\nOrderNumber: {}\r\nOrderItem-SKU: {}\r\nSQS-Message: {}\r\nError-Message: {}",
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    rawMessage,
                    e.getMessage()
            );
            throw e;
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
        logReceivedMessage(rawMessage, senderId, receiveCount);
        final var invoiceUrl = objectMapper.readValue(rawMessage, SqsMessage.class).getBody();

        try {
            final var orderNumber = extractOrderNumber(invoiceUrl);
            log.info("Adding invoice {} to order-number {}", invoiceUrl, orderNumber);

            final Map<String, Object> processVariables = new HashMap<>();
            processVariables.put(ORDER_NUMBER.getName(), orderNumber);
            processVariables.put(Variables.INVOICE_URL.getName(), invoiceUrl);

            final var result = runtimeService
                    .createMessageCorrelation(Messages.INVOICE_CREATED.getName())
                    .processInstanceBusinessKey(orderNumber)
                    .setVariables(processVariables)
                    .correlateWithResult();

            if (StringUtils.hasLength(result.getExecution().getProcessInstanceId())) {
                log.info("Invoice {} from core for order-number {} successfully received", invoiceUrl, orderNumber);
            }
        } catch (Exception e) {
            log.error("Invoice received from core message error - invoice url: {}\r\nErrorMessage: {}", invoiceUrl, e);
            throw e;
        }
    }

    private String extractOrderNumber(final String invoiceUrl) {
       final var afterLastSlash = invoiceUrl.lastIndexOf('/') + 1;

       if (afterLastSlash > 0) {
           final var minus = invoiceUrl.indexOf('-', afterLastSlash);
           if (minus != -1) {
               return invoiceUrl.substring(afterLastSlash, minus);
           }
       }

        throw new IllegalArgumentException("Cannot parse OrderNumber from invoice url: " + invoiceUrl);
    }

    private void logReceivedMessage(final String rawMessage, final String senderId, final Integer receiveCount) {
        log.info("message received: {}\r\nmessage receive count: {}\r\nmessage content: {}",
                senderId,
                receiveCount.toString(),
                rawMessage
        );
    }

    /**
     * Send message to bpmn engine
     */
    private MessageCorrelationResult sendOrderRowMessage(RowMessages itemMessages, String orderNumber, String orderItemSku) {
        return runtimeService.createMessageCorrelation(itemMessages.getName())
                .processInstanceBusinessKey(orderNumber + "#" +orderItemSku)
                .correlateWithResult();
    }

}
