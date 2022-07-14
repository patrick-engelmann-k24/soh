package de.kfzteile24.salesOrderHub.configuration;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.extension.migration.Migrator;
import org.camunda.bpm.extension.migration.plan.step.variable.strategy.ReadProcessVariable;
import org.camunda.bpm.extension.migration.plan.step.variable.strategy.WriteProcessVariable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CamundaConfig {

    @Bean
    Migrator migrator(ProcessEngine processEngine) {
        return new Migrator(processEngine);
    }

    @Bean
    ReadProcessVariable readProcessVariable() {
        return new ReadProcessVariable();
    }

    @Bean
    WriteProcessVariable writeProcessVariable() {
        return new WriteProcessVariable();
    }
}
