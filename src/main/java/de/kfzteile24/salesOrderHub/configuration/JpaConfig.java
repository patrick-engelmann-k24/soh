package de.kfzteile24.salesOrderHub.configuration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EntityScan(basePackages = {"de.kfzteile24.salesOrderHub.domain"})
@EnableJpaRepositories(basePackages = {"de.kfzteile24.salesOrderHub.repositories"})
@EnableTransactionManagement
@EnableJpaAuditing
public class JpaConfig {
}
