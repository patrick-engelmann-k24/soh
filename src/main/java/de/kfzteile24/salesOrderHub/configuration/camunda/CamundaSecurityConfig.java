package de.kfzteile24.salesOrderHub.configuration.camunda;

import org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter;
import org.camunda.bpm.engine.rest.security.auth.impl.HttpBasicAuthenticationProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import static org.camunda.bpm.engine.rest.security.auth.ProcessEngineAuthenticationFilter.AUTHENTICATION_PROVIDER_PARAM;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
class CamundaSecurityConfig {

    static final String CAMUNDA_REST_API_PATTERN = "/engine-rest/*";
    static final String FILTER_NAME_CAMUNDA_REST_BASIC_AUTH = "camunda-rest-basic-auth";

    @Bean
    @Order(HIGHEST_PRECEDENCE)
    FilterRegistrationBean<ProcessEngineAuthenticationFilter> restApiBasicAuthFilterRegistration(
            ProcessEngineAuthenticationFilter processEngineAuthenticationFilter) {
        FilterRegistrationBean<ProcessEngineAuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setName(FILTER_NAME_CAMUNDA_REST_BASIC_AUTH);
        registration.setFilter(processEngineAuthenticationFilter);
        registration.addInitParameter(AUTHENTICATION_PROVIDER_PARAM, HttpBasicAuthenticationProvider.class.getName());
        registration.addUrlPatterns(CAMUNDA_REST_API_PATTERN);
        return registration;
    }

    @Bean
    ProcessEngineAuthenticationFilter processEngineAuthenticationFilter() {
        return new ProcessEngineAuthenticationFilter();
    }
}
