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

    @Value("${ignoreCoreCreditNote}")
    private Boolean ignoreMigrationCoreSalesInvoice;

    @Value("${ignoreCoreCreditNote}")
    private Boolean ignoreMigrationCoreSalesCreditNote;

    @Value("${ignoreCoreCreditNote}")
    private Boolean ignoreMigrationCoreSalesOrder;
}
