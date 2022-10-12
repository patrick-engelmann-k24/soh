package de.kfzteile24.salesOrderHub.services.sqs;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;

import java.util.Objects;

import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.joining;

@Slf4j
public abstract class AbstractSqsReceiveService {

    @MessageExceptionHandler(Throwable.class)
    public void handleExceptions(Throwable e, Message<String> sqsMessage) {
        logErrorMessage(sqsMessage, e);
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleMethodArgumentNotValidExceptions(MethodArgumentNotValidException e) throws MethodArgumentNotValidException {
        var bindingResult = e.getBindingResult();
        if (Objects.nonNull(bindingResult)) {
            var validationErrors = bindingResult.getFieldErrors().stream()
                    .map(error -> format("{0} {1}", error.getField(), error.getDefaultMessage()))
                    .collect(joining(", "));
            log.error("Constraint validation failed. {}", validationErrors);
        } else {
            log.error("Binding result is null");
        }
        throw e;
    }

    public static void logErrorMessage(Message<String> sqsMessage, Throwable e) {
        var headers = sqsMessage.getHeaders();
        String message = format("Message Error:\r\n Queue Name: {0}\r\nReceive Count: {1}\r\nContent: {2}",
                headers.get("LogicalResourceId", String.class),
                headers.get("ApproximateReceiveCount", String.class),
                RegExUtils.removeAll(sqsMessage.getPayload(), "[\\t\\n\\r]+"));
        logErrorMessage(message, e);
    }

    public static void logErrorMessage(MessageWrapper messageWrapper, Throwable e) {
        String message = format("Message Error:\r\n Queue Name: {0}\r\nReceive Count: {1}\r\nContent: {2}",
                messageWrapper.getQueueName(),
                messageWrapper.getReceiveCount(),
                messageWrapper.getSanitizedPayload());
        logErrorMessage(message, e);
    }

    @SneakyThrows
    public static void logErrorMessage(String message, Throwable e) {
        log.error(message + "\r\nError-Message: {}", e.getMessage());
        throw e;
    }

    public static void logIncomingMessage(MessageWrapper messageWrapper) {
        log.info("Message Received: {} \r\nQueue Name: {}\r\nReceive Count: {}\r\nContent: {}",
                messageWrapper.getSenderId(),
                messageWrapper.getQueueName(),
                messageWrapper.getReceiveCount(),
                messageWrapper.getSanitizedPayload());
    }
}
