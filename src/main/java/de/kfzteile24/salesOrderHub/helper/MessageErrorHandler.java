package de.kfzteile24.salesOrderHub.helper;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.text.MessageFormat.format;

@Slf4j
@Component
public class MessageErrorHandler {

    public void logErrorMessage(String rawMessage, String queueName, Integer receiveCount, Throwable e) {
        String message = format("Message Error:\r\n Queue Name: {0}\r\nReceive Count: {1}\r\nContent: {2}",
                queueName, receiveCount, rawMessage);
        logErrorMessage(message, e);
    }

    @SneakyThrows
    public void logErrorMessage(String message, Throwable e) {
        log.error(message + "\r\nError-Message: {}", e.getMessage());
        throw e;
    }

}
