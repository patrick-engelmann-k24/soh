package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AwsSnsConfig {

    @Value("${soh.sns.topic.orderCreated}")
    String snsOrderCreatedTopic;

    @Value("${soh.sns.topic.orderCompleted}")
    String snsOrderCompletedTopic;

    @Value("${soh.sns.topic.orderCancelled}")
    String snsOrderCancelledTopic;

    @Value("${soh.sns.topic.orderItemCancelled}")
    String snsOrderItemCancelledTopic;

    @Value("${soh.sns.topic.invoiceAddressChanged}")
    String snsInvoiceAddressChangedTopic;

    @Value("${soh.sns.topic.deliveryAddressChanged}")
    String snsDeliveryAddressChanged;
}
