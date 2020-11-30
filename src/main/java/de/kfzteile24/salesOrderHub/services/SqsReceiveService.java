package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.item.ItemVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SqsReceiveService {

    final Gson gson;
    final RuntimeService runtimeService;
    final SalesOrderService salesOrderService;
    final CamundaHelper camundaHelper;

    @Value("${soh.sqs.maxMessageRetrieves}")
    private Integer maxMessageRetrieves;

    /**
     * Consume sqs for new orders from ecp shop
     *
     * @param message
     * @param senderId
     */
    @SqsListener("${soh.sqs.queue.ecpShopOrders}")
    public void queueListenerEcpShopOrders(String message, @Header("SenderId") String senderId) {
        log.info("message received: " + senderId);

        try {
            OrderJSON orderJSON = gson.fromJson(message, OrderJSON.class);
            final SalesOrder ecpSalesOrder = de.kfzteile24.salesOrderHub.domain.SalesOrder.builder()
                    .orderNumber(orderJSON.getOrderHeader().getOrderNumber())
                    .salesLocale(orderJSON.getOrderHeader().getOrigin().getLocale())
                    .originalOrder(orderJSON)
                    .build();

            salesOrderService.save(ecpSalesOrder);

            ProcessInstance result = camundaHelper.createOrderProcess(ecpSalesOrder, Messages.ORDER_RECEIVED_ECP);

            if (result != null) {
                log.info("New ecp order process started: " + result.getId());
            }
        } catch (JsonSyntaxException e) {
            log.error("ECP Order could not parsed from Json");
            log.error(e.getMessage());
        }
    }

    /**
     * Consume messages from sqs for event order item shipped
     *
     * @param message
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemShipped}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void queueListenerItemShipped(String message, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        log.info("message received: " + senderId);
        log.info("message receive count: " + receiveCount.toString());
        FulfillmentMessage fulfillmentMessage = gson.fromJson(message, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderItemMessage(
                    ItemMessages.ITEM_SHIPPED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item shipped message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item shipped message error - OrderNumber " + fulfillmentMessage.getOrderNumber() + ", OrderItem: " + fulfillmentMessage.getOrderItemSku());
            log.error(e.getMessage());
            if (receiveCount < maxMessageRetrieves) {
                //ToDo handle dead letter queue sending
                throw e;
            }
        }

    }

    /**
     * Consume messages from sqs for order payment secured
     *
     * @param message
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderPaymentSecured}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void queueListenerOrderPaymentSecured(String message, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        log.info("message received: " + senderId);
        log.info("message receive count: " + receiveCount.toString());
        CoreDataReaderEvent coreDataReaderEvent = gson.fromJson(message, CoreDataReaderEvent.class);

        try {
            MessageCorrelationResult result = runtimeService.createMessageCorrelation(Messages.ORDER_RECEIVED_PAYMENT_SECURED.getName())
                    .processInstanceBusinessKey(coreDataReaderEvent.getOrderNumber())
                    .correlateWithResult();

            if (result.getProcessInstance() != null) {
                log.info("Order payment secured message for oder number " + coreDataReaderEvent.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order payment secured message error - OrderNumber " + coreDataReaderEvent.getOrderNumber());
            log.error(e.getMessage());
            if (receiveCount < maxMessageRetrieves) {
                //ToDo handle dead letter queue sending
                throw e;
            }
        }

    }

    /**
     * Consume messages from sqs for order item transmitted to logistic
     *
     * @param message
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTransmittedToLogistic}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void queueListenerOrderItemTransmittedToLogistic(String message, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        log.info("message received: " + senderId);
        log.info("message receive count: " + receiveCount.toString());
        FulfillmentMessage fulfillmentMessage = gson.fromJson(message, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderItemMessage(
                    ItemMessages.ITEM_TRANSMITTED_TO_LOGISTICS,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item transmitted to logistic message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item transmitted to logistic message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            if (receiveCount < maxMessageRetrieves) {
                //ToDo handle dead letter queue sending
                throw e;
            }
        }
    }

    /**
     * Consume messages from sqs for event order item packing started
     *
     * @param message
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemPackingStarted}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void queueListenerOrderItemPackingStarted(String message, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        log.info("message received: " + senderId);
        log.info("message receive count: " + receiveCount.toString());
        FulfillmentMessage fulfillmentMessage = gson.fromJson(message, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderItemMessage(
                    ItemMessages.PACKING_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item packing started message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item packing started message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            if (receiveCount < maxMessageRetrieves) {
                //ToDo handle dead letter queue sending
                throw e;
            }
        }
    }

    /**
     * Consume messages from sqs for event order item tracking id received
     *
     * @param message
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTrackingIdReceived}", deletionPolicy = SqsMessageDeletionPolicy.ON_SUCCESS)
    public void queueListenerOrderItemTrackingIdReceived(String message, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        log.info("message received: " + senderId);
        log.info("message receive count: " + receiveCount.toString());
        FulfillmentMessage fulfillmentMessage = gson.fromJson(message, FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderItemMessage(
                    ItemMessages.TRACKING_ID_RECEIVED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
                    );

            if (result.getProcessInstance() != null) {
                log.info("Order item tracking id received message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item tracking id received message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            if (receiveCount < maxMessageRetrieves) {
                //ToDo handle dead letter queue sending
                throw e;
            }
        }
    }

    /**
     * Send message to bpmn engine
     *
     * @param itemMessages
     * @param orderNumber
     * @param orderItemSku
     * @return
     */
    private MessageCorrelationResult sendOrderItemMessage(ItemMessages itemMessages, String orderNumber, String orderItemSku) {
        MessageCorrelationResult result = runtimeService.createMessageCorrelation(itemMessages.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(ItemVariables.ORDER_ITEM_ID.getName(), orderItemSku)
                .correlateWithResult();

        return result;
    }
}
