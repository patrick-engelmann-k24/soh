package de.kfzteile24.salesOrderHub.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;

@Configuration
@Slf4j
public class LogbookConfiguration {

    @Bean
    public Logbook logbook() {
        return Logbook.builder()
//                .condition(exclude(requestTo("/healthCheck")))
                .correlationId(new CustomCorrelationId())
                .build();
    }

}