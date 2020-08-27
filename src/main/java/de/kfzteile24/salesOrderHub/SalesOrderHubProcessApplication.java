
package de.kfzteile24.salesOrderHub;

import com.zaxxer.hikari.HikariDataSource;
import de.kfzteile24.salesOrderHub.dao.OrderDao;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration;
import org.camunda.bpm.spring.boot.starter.configuration.impl.DefaultDatasourceConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

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
