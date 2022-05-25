package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
@ConfigurationProperties(prefix = "kfzteile.product-data-hub")
@Getter
@Setter
public class ProductDataHubConfig {

    private URI pdhUrl;

    private URI authUrl;

    private String clientId;

    private String clientSecret;
}
