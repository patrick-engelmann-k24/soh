package de.kfzteile24.salesOrderHub.services.salesorder;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowEvents;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderSqsReceiveService extends AbstractSqsReceiveService {

    private final SalesOrderService salesOrderService;
    private final SalesOrderRowService salesOrderRowService;
    private final SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    private final DropshipmentOrderService dropshipmentOrderService;
    private final SalesOrderProcessService salesOrderCreateService;

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.ecpShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerEcpShopOrders(@Validated Order message, MessageWrapper messageWrapper) {
        salesOrderCreateService.handleShopOrdersReceived(message, messageWrapper);
    }

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.bcShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerBcShopOrders(@Validated Order message, MessageWrapper messageWrapper) {
        salesOrderCreateService.handleShopOrdersReceived(message, messageWrapper);
    }

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.coreShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerCoreShopOrders(@Validated Order message, MessageWrapper messageWrapper) {
        salesOrderCreateService.handleShopOrdersReceived(message, messageWrapper);
    }

    /**
     * Consume messages from sqs for event order item shipped
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemShipped}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling ItemShipped message", dispatcher = true)
    public void queueListenerItemShipped(FulfillmentMessage message, MessageWrapper messageWrapper) {

        log.info("Received item shipped  message with order number: {} ", message.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.ROW_SHIPPED,
                message.getOrderNumber(),
                message.getOrderItemSku(),
                "Order item shipped",
                messageWrapper.getPayload(),
                RowEvents.ROW_SHIPPED);
    }

    /**
     * Consume messages from sqs for order payment secured
     */
    @SqsListener(value = "${soh.sqs.queue.orderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderPaymentSecured message", dispatcher = true)
    public void queueListenerOrderPaymentSecured(CoreDataReaderEvent message) {
        var orderNumber = message.getOrderNumber();
        log.info("Received order payment secured message with order number: {} ", orderNumber);

        Optional.of(salesOrderService.getOrderByOrderNumber(orderNumber)
                        .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber)))
                .filter(not(salesOrderPaymentSecuredService::hasOrderPaypalPaymentType))
                .ifPresentOrElse(p -> salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured(orderNumber),
                        () -> log.info("Order with order number: {} has paypal payment type. Prevent processing order" +
                                " payment secured message", orderNumber));
    }

    /**
     * Consume messages from sqs for order item transmitted to logistic
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTransmittedToLogistic}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemTransmittedToLogistic message", dispatcher = true)
    public void queueListenerOrderItemTransmittedToLogistic(FulfillmentMessage message, MessageWrapper messageWrapper) {

        log.info("Received order item transmitted to logistic message with order number: {} ", message.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.ROW_TRANSMITTED_TO_LOGISTICS,
                message.getOrderNumber(),
                message.getOrderItemSku(),
                "Order item transmitted to logistic",
                messageWrapper.getPayload(),
                RowEvents.ROW_TRANSMITTED_TO_LOGISTICS);
    }

    /**
     * Consume messages from sqs for event order item packing started
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemPackingStarted}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemPacking message", dispatcher = true)
    public void queueListenerOrderItemPackingStarted(FulfillmentMessage message, MessageWrapper messageWrapper) {

        log.info("Received order item packing message with order number: {} ", message.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.PACKING_STARTED,
                message.getOrderNumber(),
                message.getOrderItemSku(),
                "Order item packing started",
                messageWrapper.getPayload(),
                RowEvents.PACKING_STARTED);
    }

    /**
     * Consume messages from sqs for event order item tracking id received
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTrackingIdReceived}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemTrackingIdReceived message", dispatcher = true)
    public void queueListenerOrderItemTrackingIdReceived(FulfillmentMessage message, MessageWrapper messageWrapper) {

        log.info("Received order item tracking id message with order number: {} ", message.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.TRACKING_ID_RECEIVED,
                message.getOrderNumber(),
                message.getOrderItemSku(),
                "Order item tracking id received",
                messageWrapper.getPayload(),
                RowEvents.TRACKING_ID_RECEIVED);
    }

    /**
     * Consume messages from sqs for event order item tour started
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTourStarted}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling OrderItemTourStarted message", dispatcher = true)
    public void queueListenerOrderItemTourStarted(FulfillmentMessage message, MessageWrapper messageWrapper) {

        log.info("Received order item tour started message with order number: {} ", message.getOrderNumber());

        salesOrderRowService.correlateOrderRowMessage(
                RowMessages.TOUR_STARTED,
                message.getOrderNumber(),
                message.getOrderItemSku(),
                "Order item tour started",
                messageWrapper.getPayload(),
                RowEvents.TOUR_STARTED);
    }

    /**
     * Consume messages from sqs for order payment secured published by D365
     */
    @SqsListener(value = "${soh.sqs.queue.d365OrderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling d365OrderPaymentSecured message", dispatcher = true)
    public void queueListenerD365OrderPaymentSecured(OrderPaymentSecuredMessage message) {


        var orderNumbers = message.getData().getSalesOrderId().stream()
                .filter(not(dropshipmentOrderService::isDropShipmentOrder))
                .toArray(String[]::new);
        log.info("Received d365 order payment secured message with order group id: {} and order numbers: {}",
                message.getData().getOrderGroupId(), orderNumbers);

        salesOrderPaymentSecuredService.correlateOrderReceivedPaymentSecured(orderNumbers);
    }
}
