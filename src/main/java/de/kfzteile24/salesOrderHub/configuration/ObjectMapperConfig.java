package de.kfzteile24.salesOrderHub.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapperUtil;
import de.kfzteile24.salesOrderHub.services.sqs.SqsReceiveService;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.unbrokendome.jackson.beanvalidation.BeanValidationModule;

import javax.validation.ValidatorFactory;
import java.util.Arrays;

import static org.unbrokendome.jackson.beanvalidation.BeanValidationFeature.REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS;

/**
 * @author vinaya
 */
@Configuration
public class ObjectMapperConfig {

    public static final String OBJECT_MAPPER_WITH_BEAN_VALIDATION = "objectMapperWithBeanValidation";

    /**
     * Used within implementation, integration tests. Contains all the available modules injected by Spring Boot
     * excluding bean validation module
     *
     * This is a primary bean injected without qualifier:
     *
     * ObjectMapper objectMapper
     *
     * @param modules available object mapper modules
     */
    @Bean
    @Primary
    ObjectMapper objectMapper(Module[] modules) {
        return buildObjectMapper(Arrays.stream(modules)
                .filter(module -> !ClassUtils.isAssignable(module.getClass(), BeanValidationModule.class))
                .toArray(Module[]::new)
        );
    }

    /**
     * Used within implementation, integration tests. Contains all the available modules injected by Spring Boot
     * including bean validation module
     *
     * This is a secondary bean injected with qualifier:
     *
     * @see MessageWrapperUtil#setObjectMapper(ObjectMapper)
     * @see SqsReceiveService#setObjectMapper(ObjectMapper)
     *
     * @param modules available object mapper modules
     */
    @Bean(OBJECT_MAPPER_WITH_BEAN_VALIDATION)
    ObjectMapper objectMapperWithBeanValidation(Module[] modules) {
        return buildObjectMapper(modules);
    }

    /**
     * Used only within the unit(mockito) tests and test utils without Spring Boot DI context.
     * Contains no bean validation module
     */
    public ObjectMapper objectMapper() {
        return buildObjectMapper(javaTimeModule());
    }

    @Bean
    Module javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean
    Module beanValidationModule(ValidatorFactory validatorFactory) {
        return new BeanValidationModule(validatorFactory)
                .enable(REPORT_BEAN_PROPERTY_PATHS_IN_VIOLATIONS);
    }

    private ObjectMapper buildObjectMapper(Module... modules) {
        return JsonMapper.builder()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .addModules(modules)
                .build();
    }
}
