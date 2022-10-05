package de.kfzteile24.salesOrderHub.services.processmigration.exception;

public class NoProcessDefinitionFound extends RuntimeException {

    public NoProcessDefinitionFound(String message) {
        super(message);
    }
}
