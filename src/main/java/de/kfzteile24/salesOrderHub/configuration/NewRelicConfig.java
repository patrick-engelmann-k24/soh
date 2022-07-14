package de.kfzteile24.salesOrderHub.configuration;

import com.newrelic.api.agent.Agent;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class NewRelicConfig {

    @Bean
    Agent agent() {
        return NewRelic.getAgent();
    }

    @Bean
    Insights insights(Agent agent) {
        return agent.getInsights();
    }
}