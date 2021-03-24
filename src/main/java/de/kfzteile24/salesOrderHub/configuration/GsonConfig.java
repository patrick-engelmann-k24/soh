package de.kfzteile24.salesOrderHub.configuration;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.kfzteile24.salesOrderHub.configuration.gson.LocalDateTimeAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.text.DateFormat;
import java.time.LocalDateTime;

@Configuration
public class GsonConfig {
    /**
     * Gson config for k24 order json
     *
     * @return the message decoder
     */
    @Bean
    @Primary
    Gson gsonBeanProvider() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .setDateFormat(DateFormat.LONG, DateFormat.LONG)
                .create();
    }

    /**
     * Gson config for message header
     *
     * @return the messageHeader gson decoder
     */
    @Bean("messageHeader")
    Gson gsonBeanProviderMessageHeader() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter().nullSafe())
                .setDateFormat(DateFormat.LONG, DateFormat.LONG)
                .create();

    }

}
