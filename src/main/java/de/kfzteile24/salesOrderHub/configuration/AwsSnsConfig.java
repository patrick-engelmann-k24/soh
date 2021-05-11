package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AwsSnsConfig {

    @Value("${soh.sns.topic.orderCreated}")
    private String snsOrderCreatedTopic;

    @Value("${soh.sns.topic.orderCompleted}")
    private String snsOrderCompletedTopic;

    @Value("${soh.sns.topic.orderCancelled}")
    private String snsOrderCancelledTopic;

    @Value("${soh.sns.topic.orderItemCancelled}")
    private String snsOrderItemCancelledTopic;

    @Value("${soh.sns.topic.invoiceAddressChanged}")
    private String snsInvoiceAddressChangedTopic;

    @Value("${soh.sns.topic.deliveryAddressChanged}")
    private String snsDeliveryAddressChanged;
}
