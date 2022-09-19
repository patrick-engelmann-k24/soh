package de.kfzteile24.salesOrderHub.configuration;

import brave.baggage.BaggageField;
import brave.baggage.CorrelationScopeConfig;
import brave.baggage.CorrelationScopeDecorator;
import brave.context.slf4j.MDCScopeDecorator;
import brave.propagation.CurrentTraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!test")
public class SleuthConfig {

    public static final String TRACE_ID_NAME = "x-business-key";

    @Bean
    BaggageField businessKeyField() {
        return BaggageField.create(TRACE_ID_NAME);
    }

    @Bean
    CurrentTraceContext.ScopeDecorator mdcScopeDecorator() {
        var baggageFields = List.of(BaggageField.create(TRACE_ID_NAME));
        CorrelationScopeDecorator.Builder scopeDecoratorBuilder = MDCScopeDecorator.newBuilder()
                .clear();
        baggageFields.stream()
                .map(this::correlationField)
                .forEach(scopeDecoratorBuilder::add);
        return scopeDecoratorBuilder.build();
    }

    private CorrelationScopeConfig.SingleCorrelationField correlationField(BaggageField baggageField) {
        return CorrelationScopeConfig.SingleCorrelationField
                .newBuilder(baggageField)
                .flushOnUpdate()
                .build();
    }
}
