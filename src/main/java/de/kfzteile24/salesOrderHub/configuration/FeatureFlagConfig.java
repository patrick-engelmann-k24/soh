package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class FeatureFlagConfig {

    @Value("${ignoreCoreSalesInvoice}")
    private Boolean ignoreCoreSalesInvoice;

    @Value("${ignoreCoreCreditNote}")
    private Boolean ignoreCoreCreditNote;

    @Value("${ignoreMigrationCoreSalesInvoice}")
    private Boolean ignoreMigrationCoreSalesInvoice;

    @Value("${ignoreMigrationCoreSalesCreditNote}")
    private Boolean ignoreMigrationCoreSalesCreditNote;

    @Value("${ignoreMigrationCoreSalesOrder}")
    private Boolean ignoreMigrationCoreSalesOrder;

    @Value("${ignoreSalesOrderSplitter}")
    private Boolean ignoreSalesOrderSplitter;

    @Value("${ignoreSetDissolvement}")
    private Boolean ignoreSetDissolvement;

    @Value("preventSetProcessing")
    private Boolean preventSetProcessing;
}
