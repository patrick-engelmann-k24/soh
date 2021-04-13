package de.kfzteile24.salesOrderHub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.MarketingSalesOrder;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import java.text.MessageFormat;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.cloud.aws.messaging.core.NotificationMessagingTemplate;
import org.springframework.stereotype.Service;

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
    public void send(String snsTopic, String subject, MarketingSalesOrder marketingSalesOrder) {
        notificationMessagingTemplate.sendNotification(snsTopic,
                                                       objectMapper.writeValueAsString(marketingSalesOrder),
                                                       subject);
    }

    public void sendOrder(String topic, String subject, String orderNumber) throws Exception {
        SalesOrder salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
            .orElseThrow(() -> new SalesOrderNotFoundException(MessageFormat.format(
                "Sales order not found for the given order number {0} ", orderNumber)));
        MarketingSalesOrder marketingSalesOrder = MarketingSalesOrder.builder()
                                                                     .order(salesOrder.getOriginalOrder())
                                                                     .recurringOrder(salesOrder.isRecurringOrder())
                                                                     .build();
        send(topic, subject, marketingSalesOrder);
        }
}
