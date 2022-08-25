package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SQSNamesConfig {

    @Value("${soh.sqs.queue.coreSalesInvoiceCreated}")
    private String coreSalesInvoiceCreated;
}
