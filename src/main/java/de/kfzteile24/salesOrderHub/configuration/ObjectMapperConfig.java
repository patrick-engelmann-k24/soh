package de.kfzteile24.salesOrderHub.configuration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author vinaya
 */
@Configuration
public class ObjectMapperConfig {

  /**
   * Object mapper configuration for deserializing Ecp order message body
   * @return
   */
  @Bean("messageBodyMapper")
  public ObjectMapper getMapperForMessageBody(){
    ObjectMapper mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    mapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

}
