package de.kfzteile24.salesOrderHub.services.salesorder;

import com.newrelic.api.agent.Trace;
import de.kfzteile24.salesOrderHub.dto.sns.CoreDataReaderEvent;
import de.kfzteile24.salesOrderHub.dto.sns.OrderPaymentSecuredMessage;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderCancelledService;
import de.kfzteile24.salesOrderHub.services.SalesOrderPaymentSecuredService;
import de.kfzteile24.salesOrderHub.services.SalesOrderProcessService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.AbstractSqsReceiveService;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.function.Predicate.not;
import static org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderSqsReceiveService extends AbstractSqsReceiveService {

    private final SalesOrderService salesOrderService;
    private final SalesOrderPaymentSecuredService salesOrderPaymentSecuredService;
    private final SalesOrderProcessService salesOrderCreateService;
    private final SalesOrderCancelledService salesOrderCancelledService;

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.ecpShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerEcpShopOrders(Order message, MessageWrapper messageWrapper) {
        salesOrderCreateService.handleShopOrdersReceived(message, messageWrapper);
    }

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.bcShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerBcShopOrders(Order message, MessageWrapper messageWrapper) {
        salesOrderCreateService.handleShopOrdersReceived(message, messageWrapper);
    }

    /**
     * Consume sqs for new orders from ecp, bc and core shops
     */
    @SqsListener(value = {"${soh.sqs.queue.coreShopOrders}"}, deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling shop order message", dispatcher = true)
    public void queueListenerCoreShopOrders(Order message, MessageWrapper messageWrapper) {
        salesOrderCreateService.handleShopOrdersReceived(message, messageWrapper);
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
     * Consume messages from sqs for order payment secured published by D365
     */
    @SqsListener(value = "${soh.sqs.queue.d365OrderPaymentSecured}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling d365OrderPaymentSecured message", dispatcher = true)
    public void queueListenerD365OrderPaymentSecured(OrderPaymentSecuredMessage message, MessageWrapper messageWrapper) {
        salesOrderPaymentSecuredService.handleD365OrderPaymentSecured(message, messageWrapper);
    }


    /**
     * Consume messages from sqs for core sales order cancelled
     */
    @SqsListener(value = "${soh.sqs.queue.coreSalesOrderCancelled}", deletionPolicy = ON_SUCCESS)
    @Trace(metricName = "Handling coreSalesOrderCancelled message", dispatcher = true)
    public void queueListenerCoreSalesOrderCancelled(CoreSalesOrderCancelledMessage message, MessageWrapper messageWrapper) {
        salesOrderCancelledService.handleCoreSalesOrderCancelled(message, messageWrapper);
    }
}
