package de.kfzteile24.salesOrderHub.services.sqs.exception;

public class InvalidOrderJsonException extends RuntimeException {

    public InvalidOrderJsonException(String message) {
        super(message);
    }

    public InvalidOrderJsonException(Throwable cause) {
        super(cause.getMessage());
    }
}
