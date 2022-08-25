package de.kfzteile24.salesOrderHub.services.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.dto.sqs.SqsMessage;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.sqs.exception.InvalidOrderJsonException;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolationException;
import java.util.Optional;

import static de.kfzteile24.salesOrderHub.configuration.ObjectMapperConfig.OBJECT_MAPPER_WITH_BEAN_VALIDATION;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageWrapperUtil {

    private final ObjectMapper objectMapperNoValidation;
    private final SleuthHelper sleuthHelper;
    private ObjectMapper objectMapper;

    @SneakyThrows(JsonProcessingException.class)
    public <T> MessageWrapper<T> create(String rawMessage, Class<T> messageType) {
        SqsMessage sqsMessage = objectMapper.readValue(rawMessage, SqsMessage.class);
        return MessageWrapper.<T>builder()
                .sqsMessage(sqsMessage)
                .message(getMessage(messageType, sqsMessage))
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

    private <T> T getMessage(Class<T> messageType, SqsMessage sqsMessage) {
        T json;
        String errormessage = StringUtils.EMPTY;
        try {
            json = objectMapper.readValue(sqsMessage.getBody(), messageType);
            updateTraceId(json);
            return json;
        } catch (ConstraintViolationException e1) {
            updateTraceId(messageType, sqsMessage, e1.getMessage());
            throw new InvalidOrderJsonException(errormessage);
        } catch (JsonProcessingException e2) {
            updateTraceId(messageType, sqsMessage, e2.getCause().getMessage());
            throw new InvalidOrderJsonException(e2);
        }
    }

    @SneakyThrows({ConstraintViolationException.class, JsonProcessingException.class})
    private <T> void updateTraceId(Class<T> messageType, SqsMessage sqsMessage, String errormessage) {
        T json = objectMapperNoValidation.readValue(sqsMessage.getBody(), messageType);
        updateTraceId(json);
        log.error(errormessage);
    }

    private <T> void updateTraceId(T json) {

        Optional.ofNullable(json)
                .filter(j -> j.getClass().isAssignableFrom(Order.class))
                .map(Order.class::cast)
                .ifPresent(order -> sleuthHelper.updateTraceId(order.getOrderHeader().getOrderNumber()));
    }
}
