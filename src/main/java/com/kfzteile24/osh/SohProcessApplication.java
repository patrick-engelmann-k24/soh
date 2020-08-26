
package com.kfzteile24.osh;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class SohProcessApplication {

    public static void main(String... args) {
        SpringApplication.run(SohProcessApplication.class, args);
    }
}
