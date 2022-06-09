package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "kfzteile.pricing-service")
@Getter
@Setter
public class PricingServiceConfig {
    private URI url;
    private String apiKey;
}