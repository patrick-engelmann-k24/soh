package de.kfzteile24.salesOrderHub.services;

import com.amazonaws.services.sns.AmazonSNS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class SnsPublishService {

    @Autowired
    private NotificationMessagingTemplate notificationMessagingTemplate;

    public void send(String snsTopic, String subject, String message) {
        notificationMessagingTemplate.sendNotification(snsTopic, message, subject);
    }
}
