package de.kfzteile24.salesOrderHub.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Value("${swagger.api.title}")
    private String title;

    @Value("${swagger.api.description}")
    private String description;

    @Value("${swagger.api.version}")
    private String version;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title(title)
                        .description(description)
                        .version(version));
    }
}
