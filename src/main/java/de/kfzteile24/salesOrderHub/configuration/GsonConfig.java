package de.kfzteile24.salesOrderHub.configuration;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.DateFormat;

@Configuration
public class GsonConfig {
    /**
     * Gson config for k24 order json
     *
     * @return
     */
    @Bean
    @Primary
    Gson gsonBeanProvider() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .setDateFormat(DateFormat.LONG, DateFormat.LONG)
                .create();
    }

    /**
     * Gson config for message header
     *
     * @return
     */
    @Bean
    @Qualifier("messageHeader")
    Gson gsonBeanProviderMessageHeader() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .setDateFormat(DateFormat.LONG, DateFormat.LONG)
                .create();

    }

}
