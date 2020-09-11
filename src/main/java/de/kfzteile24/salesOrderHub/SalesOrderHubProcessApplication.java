package de.kfzteile24.salesOrderHub;

import com.zaxxer.hikari.HikariDataSource;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@EnableProcessApplication
public class SalesOrderHubProcessApplication {

    public static void main(String... args) {
        SpringApplication.run(SalesOrderHubProcessApplication.class, args);
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public HikariDataSource dataSourceSOH() {
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

}
