package de.kfzteile24.salesOrderHub.services.dropshipment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderBookedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnConfirmedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentPurchaseOrderReturnNotifiedMessage;
import de.kfzteile24.salesOrderHub.dto.sns.DropshipmentShipmentConfirmedMessage;
import de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class DropshipmentSqsReceiveService extends AbstractSqsReceiveService {

    private final DropshipmentOrderService dropshipmentOrderService;

    /**
     * Consume messages from sqs for dropshipment shipment confirmed published by P&R
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentShipmentConfirmed}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling dropshipment shipment confirmed message", dispatcher = true)
    public void queueListenerDropshipmentShipmentConfirmed(
            DropshipmentShipmentConfirmedMessage message) throws JsonProcessingException {

        log.info("Received dropshipment shipment confirmed message with order number: {}", message.getSalesOrderNumber());
        dropshipmentOrderService.handleDropShipmentOrderTrackingInformationReceived(message);
    }

    /**
     * Consume messages from sqs for dropshipment purchase order booked
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderBooked}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling Dropshipment Purchase Order Booked message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderBooked(DropshipmentPurchaseOrderBookedMessage message) {

        log.info("Received drop shipment order purchased booked message with Sales Order Number: {}, External Order " +
                        "Number: {}",
                message.getSalesOrderNumber(), message.getExternalOrderNumber());
        dropshipmentOrderService.handleDropShipmentOrderConfirmed(message);
    }

    /**
     * Consume messages from sqs for dropshipment purchase order return confirmed
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderReturnConfirmed}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling Dropshipment Purchase Order Return Confirmed Message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderReturnConfirmed(
            DropshipmentPurchaseOrderReturnConfirmedMessage message) {

        log.info("Received dropshipment purchase order return confirmed message with Sales Order Number: {}, External" +
                        " Order NUmber: {}",
                message.getSalesOrderNumber(), message.getExternalOrderNumber());
        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnConfirmed(message);
    }

    /**
     * Consume messages from sqs for dropshipment purchase order booked
     */
    @SqsListener(value = "${soh.sqs.queue.dropshipmentPurchaseOrderReturnNotified}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling Dropshipment Purchase Order Return Notified message", dispatcher = true)
    public void queueListenerDropshipmentPurchaseOrderReturnNotified(
            DropshipmentPurchaseOrderReturnNotifiedMessage message, MessageWrapper messageWrapper) {

        log.info("Received dropshipment purchase order return notified message with " +
                        "Sales Order Number: {}, External Order Number: {}, Sender Id: {}, Received Count {}",
                message.getSalesOrderNumber(),
                message.getExternalOrderNumber(),
                messageWrapper.getSenderId(),
                messageWrapper.getReceiveCount());
        dropshipmentOrderService.handleDropshipmentPurchaseOrderReturnNotified(message);
    }
}
