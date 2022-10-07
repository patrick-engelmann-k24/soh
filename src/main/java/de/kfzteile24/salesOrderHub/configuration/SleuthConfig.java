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

import static de.kfzteile24.salesOrderHub.constants.SOHConstants.TRACE_ID_NAME;

@Configuration
@Profile("!test")
public class SleuthConfig {

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
