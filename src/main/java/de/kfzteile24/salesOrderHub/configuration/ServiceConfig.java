package de.kfzteile24.salesOrderHub.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ServiceConfig {

    @Value("${infrastructureEnvironment}")
    private String environment;
}
