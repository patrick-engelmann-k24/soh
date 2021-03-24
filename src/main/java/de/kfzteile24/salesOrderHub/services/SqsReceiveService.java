package de.kfzteile24.salesOrderHub.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowMessages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.FulfillmentMessage;
import de.kfzteile24.salesOrderHub.dto.sqs.EcpOrder;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.MessageCorrelationResult;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
public class SqsReceiveService {

    private final Gson gson;
    private final Gson messageHeader;

    private final RuntimeService runtimeService;
    private final SalesOrderService salesOrderService;
    private final CamundaHelper camundaHelper;

    private Integer maxMessageRetrieves;

    public SqsReceiveService(
        final Gson gson,
        final RuntimeService runtimeService,
        final SalesOrderService salesOrderService,
        final CamundaHelper camundaHelper,
        final @Value("${soh.sqs.maxMessageRetrieves}") Integer maxMessageRetrieves,
        final @Qualifier("messageHeader") Gson messageHeader
    ) {
        this.gson = gson;
        this.runtimeService = runtimeService;
        this.salesOrderService = salesOrderService;
        this.camundaHelper = camundaHelper;
        this.maxMessageRetrieves = maxMessageRetrieves;
        this.messageHeader = messageHeader;
    }

    /**
     * Consume sqs for new orders from ecp shop
     *
     * @param rawMessage
     * @param senderId
     */
    @SqsListener("${soh.sqs.queue.ecpShopOrders}")
    public void queueListenerEcpShopOrders(String rawMessage, @Header("SenderId") String senderId) {
        log.info("message received: " + senderId);

        try {
            String message = messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage();
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
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemShipped}", deletionPolicy = ON_SUCCESS)
    public void queueListenerItemShipped(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        FulfillmentMessage fulfillmentMessage = gson.fromJson(messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage(), FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.ROW_SHIPPED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item shipped message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item shipped message error - OrderNumber " + fulfillmentMessage.getOrderNumber() + ", OrderItem: " + fulfillmentMessage.getOrderItemSku());
            log.error(e.getMessage());
            throw e;
        }

    }

    /**
     * Consume messages from sqs for order payment secured
     *
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    public void queueListenerOrderPaymentSecured(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        CoreDataReaderEvent coreDataReaderEvent = gson.fromJson(messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage(), CoreDataReaderEvent.class);

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
            throw e;
        }

    }

    /**
     * Consume messages from sqs for order item transmitted to logistic
     *
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTransmittedToLogistic}", deletionPolicy = ON_SUCCESS)
    public void queueListenerOrderItemTransmittedToLogistic(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        FulfillmentMessage fulfillmentMessage = gson.fromJson(messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage(), FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.ROW_TRANSMITTED_TO_LOGISTICS,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item transmitted to logistic message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item transmitted to logistic message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Consume messages from sqs for event order item packing started
     *
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemPackingStarted}", deletionPolicy = ON_SUCCESS)
    public void queueListenerOrderItemPackingStarted(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        FulfillmentMessage fulfillmentMessage = gson.fromJson(messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage(), FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.PACKING_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item packing started message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item packing started message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Consume messages from sqs for event order item tracking id received
     *
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTrackingIdReceived}", deletionPolicy = ON_SUCCESS)
    public void queueListenerOrderItemTrackingIdReceived(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        FulfillmentMessage fulfillmentMessage = gson.fromJson(messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage(), FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.TRACKING_ID_RECEIVED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
                    );

            if (result.getProcessInstance() != null) {
                log.info("Order item tracking id received message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item tracking id received message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Consume messages from sqs for event order item tour started
     *
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.orderItemTourStarted}", deletionPolicy = ON_SUCCESS)
    public void queueListenerOrderItemTourStarted(String rawMessage, @Header("SenderId") String senderId, @Header("ApproximateReceiveCount") Integer receiveCount) {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        FulfillmentMessage fulfillmentMessage = gson.fromJson(messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage(), FulfillmentMessage.class);

        try {
            MessageCorrelationResult result = sendOrderRowMessage(
                    RowMessages.TOUR_STARTED,
                    fulfillmentMessage.getOrderNumber(),
                    fulfillmentMessage.getOrderItemSku()
            );

            if (result.getProcessInstance() != null) {
                log.info("Order item tour started message for oder number " + fulfillmentMessage.getOrderNumber() + " successfully received");
            }
        } catch (Exception e) {
            log.error("Order item tour started message error - OrderNumber " + fulfillmentMessage.getOrderNumber());
            log.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Consume messages from sqs for event invoice from core
     *
     * @param rawMessage
     * @param senderId
     * @param receiveCount
     */
    @SqsListener(value = "${soh.sqs.queue.invoicesFromCore}", deletionPolicy = ON_SUCCESS)
    public void queueListenerInvoiceReceivedFromCore(
            String rawMessage,
            @Header("SenderId") String senderId,
            @Header("ApproximateReceiveCount") Integer receiveCount)
    {
        logReceivedMessage(rawMessage, senderId, receiveCount);
        final String invoiceUrl = messageHeader.fromJson(rawMessage, EcpOrder.class).getMessage();

        try {
            final var orderNumber = extractOrderNumber(invoiceUrl);
            log.info("Adding invoice {} to orderNumber {}", invoiceUrl, orderNumber);
            final MessageCorrelationResult result = runtimeService
                    .createMessageCorrelation(Messages.INVOICE_CREATED.getName())
                    .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                    .setVariable(Variables.INVOICE_URL.getName(), invoiceUrl)
                    .correlateWithResult();

            if (result.getProcessInstance() != null) {
                log.info("Invoice " + invoiceUrl + " from core successfully received");
            }
        } catch (Exception e) {
            log.error("Invoice received from core message error - invoice url " + invoiceUrl);
            log.error(e.getMessage());
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
        log.info("message received: " + senderId);
        log.info("message receive count: " + receiveCount.toString());
        log.info("message content: " + rawMessage);
    }

    /**
     * Send message to bpmn engine
     *
     * @param itemMessages
     * @param orderNumber
     * @param orderItemSku
     * @return
     */
    private MessageCorrelationResult sendOrderRowMessage(RowMessages itemMessages, String orderNumber, String orderItemSku) {
        MessageCorrelationResult result = runtimeService.createMessageCorrelation(itemMessages.getName())
                .processInstanceVariableEquals(Variables.ORDER_NUMBER.getName(), orderNumber)
                .processInstanceVariableEquals(RowVariables.ORDER_ROW_ID.getName(), orderItemSku)
                .correlateWithResult();

        return result;
    }
}
