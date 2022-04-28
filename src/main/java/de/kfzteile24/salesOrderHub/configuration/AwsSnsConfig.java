package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class AwsSnsConfig {

    @Value("${soh.sns.topic.orderCreatedV2}")
    private String snsOrderCreatedTopicV2;

    @Value("${soh.sns.topic.orderCompleted}")
    private String snsOrderCompletedTopic;

    @Value("${soh.sns.topic.orderCancelled}")
    private String snsOrderCancelledTopic;

    @Value("${soh.sns.topic.orderItemCancelled}")
    private String snsOrderItemCancelledTopic;

    @Value("${soh.sns.topic.orderRowsCancelled}")
    private String snsOrderRowsCancelledTopic;

    @Value("${soh.sns.topic.invoiceAddressChanged}")
    private String snsInvoiceAddressChangedTopic;

    @Value("${soh.sns.topic.deliveryAddressChanged}")
    private String snsDeliveryAddressChanged;

    @Value("${soh.sns.topic.salesOrderRowCancelled}")
    private String snsSalesOrderRowCancelled;

    @Value("${soh.sns.topic.salesOrderCancelled}")
    private String snsSalesOrderCancelled;

    @Value("${soh.sns.topic.orderInvoiceCreatedV1}")
    private String snsOrderInvoiceCreatedV1;

    @Value("${soh.sns.topic.shipmentConfirmedV1}")
    private String snsShipmentConfirmedV1;

    @Value("${soh.sns.topic.returnOrderCreatedV1}")
    private String snsReturnOrderCreatedV1;

    @Value("${soh.sns.topic.creditNoteReceivedV1}")
    private String snsCreditNoteReceivedV1;
}
