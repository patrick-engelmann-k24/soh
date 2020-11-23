package de.kfzteile24.salesOrderHub.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SqsReceiveService {

    //@SqsListener(value = "https://sqs.eu-central-1.amazonaws.com/967623133951/dev-soh-ecp-shop-orders-v1")
    @SqsListener("${soh.sqs.queue.ecpShopOrders}")
    public void queueListenerEcpShopOrders(String message, @Header("SenderId") String senderId) {
        log.info("message received: " + senderId);
        log.info(message);
    }
}
