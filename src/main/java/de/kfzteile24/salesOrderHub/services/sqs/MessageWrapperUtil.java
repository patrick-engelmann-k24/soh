package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;

@Component
public class MessageWrapperUtil {

    private ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public <T> MessageWrapper<T> create(String rawMessage, Class<T> messageType) {
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        return MessageWrapper.<T>builder()
                .sqsMessage(sqsMessage)
                .message(objectMapper.readValue(sqsMessage.getBody(), messageType))
                .rawMessage(rawMessage)
                .build();
    }

    public <T> T createMessage(String rawMessage, Class<T> messageType) {
        return create(rawMessage, messageType).getMessage();
    }

    @Autowired
    public void setObjectMapper(@Qualifier(OBJECT_MAPPER_WITH_BEAN_VALIDATION) ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
}
