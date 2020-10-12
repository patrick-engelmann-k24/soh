package de.kfzteile24.salesOrderHub.services;

import com.amazonaws.services.sns.AmazonSNS;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SnsPublishService {

    @Autowired
    private NotificationMessagingTemplate notificationMessagingTemplate;


    @Autowired
    SalesOrderService salesOrderService;

    public void send(String snsTopic, String subject, String message) {
        notificationMessagingTemplate.sendNotification(snsTopic, message, subject);
    }

    public void sendOrder(String topic, String subject, String orderId) throws Exception {
        Optional<SalesOrder> salesOrderOptional = salesOrderService.getOrderByOrderNumber(orderId);
        if (salesOrderOptional.isPresent()) {
            SalesOrder salesOrder = salesOrderOptional.get();
            send(topic, subject, salesOrder.getOriginalOrder());
        } else {
            throw new Exception("no sales order found");
        }
    }
}
