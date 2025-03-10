package de.kfzteile24.salesOrderHub.modeltests;

import org.camunda.bpm.extension.process_test_coverage.spring_test.ProcessEngineCoverageProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class CoverageTestConfiguration {

    @Bean
    public ProcessEngineCoverageProperties processEngineCoverageProperties() {
        return ProcessEngineCoverageProperties.builder()
                .withDetailedCoverageLogging()
                //.assertClassCoverageAtLeast(0.8)
                .build();
    }

}
