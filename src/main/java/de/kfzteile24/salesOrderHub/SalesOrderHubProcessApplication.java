package de.kfzteile24.salesOrderHub;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class SalesOrderHubProcessApplication {

    public static void main(String... args) {
        SpringApplication.run(SalesOrderHubProcessApplication.class, args);
    }
}
