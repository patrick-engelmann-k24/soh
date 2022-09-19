package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SQSNamesConfig {

    @Value("${soh.sqs.queue.coreSalesInvoiceCreated}")
    private String coreSalesInvoiceCreated;

    @Value("${soh.sqs.queue.coreSalesCreditNoteCreated}")
    private String coreSalesCreditNoteCreated;

    @Value("${soh.sqs.queue.parcelShipped}")
    private String parcelShipped;

    @Value("${soh.sqs.queue.ecpShopOrders}")
    private String ecpShopOrders;

    @Value("${soh.sqs.queue.bcShopOrders}")
    private String bcShopOrders;

    @Value("${soh.sqs.queue.coreShopOrders}")
    private String coreShopOrders;

}
