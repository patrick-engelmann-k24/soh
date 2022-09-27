package de.kfzteile24.salesOrderHub.services.salesorder;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.configuration.SQSNamesConfig;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderSqsReceiveService {

    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final SalesOrderProcessService salesOrderCreateService;
    private final MessageWrapperUtil messageWrapperUtil;
    private final SQSNamesConfig sqsNamesConfig;
    private final MessageErrorHandler messageErrorHandler;

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
     * Consume messages from sqs for event order item shipped
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemShipped}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling ItemShipped message", dispatcher = true)
    public void queueListenerItemShipped(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        try {
            var messageWrapper = messageWrapperUtil.create(rawMessage, FulfillmentMessage.class);
            FulfillmentMessage fulfillmentMessage = messageWrapper.getMessage();
            log.info("Received item shipped  message with order number: {} ", fulfillmentMessage.getOrderNumber());

            salesOrderRowService.correlateOrderRowMessage(
                    RowMessages.ROW_SHIPPED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    "Order item shipped",
                    rawMessage,
                    RowEvents.ROW_SHIPPED);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
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
            var messageWrapper = messageWrapperUtil.create(rawMessage, CoreDataReaderEvent.class);
            CoreDataReaderEvent coreDataReaderEvent = messageWrapper.getMessage();

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
     * Consume messages from sqs for order item transmitted to logistic
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTransmittedToLogistic}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemTransmittedToLogistic message", dispatcher = true)
    public void queueListenerOrderItemTransmittedToLogistic(String rawMessage,
                                                            @Header("SenderId") String senderId, @Header(
                                                                    "ApproximateReceiveCount") Integer receiveCount) {
        try {
            var messageWrapper = messageWrapperUtil.create(rawMessage, FulfillmentMessage.class);
            FulfillmentMessage fulfillmentMessage = messageWrapper.getMessage();
            log.info("Received order item transmitted to logistic message with order number: {} ",
                    fulfillmentMessage.getOrderNumber());

            salesOrderRowService.correlateOrderRowMessage(
                    RowMessages.ROW_TRANSMITTED_TO_LOGISTICS,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    "Order item transmitted to logistic",
                    rawMessage,
                    RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for event order item packing started
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemPackingStarted}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemPacking message", dispatcher = true)
    public void queueListenerOrderItemPackingStarted(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        try {
            var messageWrapper = messageWrapperUtil.create(rawMessage, FulfillmentMessage.class);
            FulfillmentMessage fulfillmentMessage = messageWrapper.getMessage();
            log.info("Received order item packing message with order number: {} ", fulfillmentMessage.getOrderNumber());

            salesOrderRowService.correlateOrderRowMessage(
                    RowMessages.PACKING_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    "Order item packing started",
                    rawMessage,
                    RowEvents.PACKING_STARTED);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for event order item tracking id received
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTrackingIdReceived}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemTrackingIdReceived message", dispatcher = true)
    public void queueListenerOrderItemTrackingIdReceived(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        try {
            var messageWrapper = messageWrapperUtil.create(rawMessage, FulfillmentMessage.class);
            FulfillmentMessage fulfillmentMessage = messageWrapper.getMessage();
            log.info("Received order item tracking id message with order number: {} ", fulfillmentMessage.getOrderNumber());

            salesOrderRowService.correlateOrderRowMessage(
                    RowMessages.TRACKING_ID_RECEIVED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    "Order item tracking id received",
                    rawMessage,
                    RowEvents.TRACKING_ID_RECEIVED);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
    }

    /**
     * Consume messages from sqs for event order item tour started
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTourStarted}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemTourStarted message", dispatcher = true)
    public void queueListenerOrderItemTourStarted(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount
    ) {
        try {
            var messageWrapper = messageWrapperUtil.create(rawMessage, FulfillmentMessage.class);
            FulfillmentMessage fulfillmentMessage = messageWrapper.getMessage();
            log.info("Received order item tour started message with order number: {} ",
                    fulfillmentMessage.getOrderNumber());

            salesOrderRowService.correlateOrderRowMessage(
                    RowMessages.TOUR_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku(),
                    "Order item tour started",
                    rawMessage,
                    RowEvents.TOUR_STARTED);
        } catch (Exception e) {
            messageErrorHandler.logErrorMessage(rawMessage, senderId, receiveCount, e);
        }
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
            var messageWrapper = messageWrapperUtil.create(rawMessage, OrderPaymentSecuredMessage.class);
            OrderPaymentSecuredMessage orderPaymentSecuredMessage = messageWrapper.getMessage();

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
}
