package de.kfzteile24.salesOrderHub.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.HeaderFilters;
import org.zalando.logbook.Logbook;

import static org.zalando.logbook.Conditions.exclude;
import static org.zalando.logbook.Conditions.requestTo;

@Configuration
public class LogbookConfiguration {

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
                .condition(exclude(requestTo("/healthCheck"), requestTo("/api/swagger-ui/**"), requestTo("/api-docs/**")))
                .headerFilter(HeaderFilters.replaceCookies("AWSALB"::equals, "XXX"))
                .headerFilter(HeaderFilters.replaceCookies("AWSALBCORS"::equals, "XXX"))
                .correlationId(new CustomCorrelationId())
                .build();
    }

}