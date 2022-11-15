package de.kfzteile24.salesOrderHub.configuration;

import de.kfzteile24.salesOrderHub.helper.CustomValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.Validator;

@Configuration
public class ValidatorConfiguration {

    @Bean
    CustomValidator customValidator(Validator validator) {
        return new CustomValidator(validator);
    }
}