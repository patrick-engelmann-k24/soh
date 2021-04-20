package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.SalesOrderInfo;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;

@Service
@RequiredArgsConstructor
public class SnsPublishService {

    @NonNull
    private final NotificationMessagingTemplate notificationMessagingTemplate;
    @NonNull
    private  final SalesOrderService salesOrderService;
    @NonNull
    private final ObjectMapper objectMapper;

    @SneakyThrows({JsonProcessingException.class})
    public void send(String snsTopic, String subject, SalesOrderInfo salesOrderInfo) {
        notificationMessagingTemplate.sendNotification(snsTopic,
                                                       objectMapper.writeValueAsString(
                                                           salesOrderInfo),
                                                       subject);
    }

    public void sendOrder(String topic, String subject, String orderNumber) throws Exception {
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
            .orElseThrow(() -> new SalesOrderNotFoundException(MessageFormat.format(
                "Sales order not found for the given order number {0} ", orderNumber)));
        SalesOrderInfo salesOrderInfo = SalesOrderInfo.builder()
                                                       .order(salesOrder.getLatestJson())
                                                       .recurringOrder(salesOrder.isRecurringOrder())
                                                       .build();
        send(topic, subject, salesOrderInfo);
        }
}
