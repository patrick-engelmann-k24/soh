package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.helper.MessageErrorHandler;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentSqsReceiveService {

    private final DropshipmentOrderService dropshipmentOrderService;
    private final MessageWrapperUtil messageWrapperUtil;
    private final MessageErrorHandler messageErrorHandler;

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
            var messageWrapper = messageWrapperUtil.create(rawMessage, DropshipmentShipmentConfirmedMessage.class);
            DropshipmentShipmentConfirmedMessage shipmentConfirmedMessage = messageWrapper.getMessage();

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
            var messageWrapper = messageWrapperUtil.create(rawMessage, DropshipmentPurchaseOrderBookedMessage.class);
            DropshipmentPurchaseOrderBookedMessage message = messageWrapper.getMessage();

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
            var messageWrapper = messageWrapperUtil.create(rawMessage, DropshipmentPurchaseOrderReturnConfirmedMessage.class);
            DropshipmentPurchaseOrderReturnConfirmedMessage message = messageWrapper.getMessage();

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
}
