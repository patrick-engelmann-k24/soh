package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageWrapperUtil {

    private final ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public <T> MessageWrapper<T> create(String rawMessage, Class<T> messageType) {
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        return MessageWrapper.<T>builder()
                .sqsMessage(sqsMessage)
                .message(objectMapper.readValue(sqsMessage.getBody(), messageType))
                .rawMessage(rawMessage)
                .build();
    }
}
