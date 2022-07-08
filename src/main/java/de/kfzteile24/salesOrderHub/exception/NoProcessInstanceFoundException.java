package de.kfzteile24.salesOrderHub.exception;

public class NoProcessInstanceFoundException extends RuntimeException {

    public NoProcessInstanceFoundException(String message) {
        super(message);
    }

}
