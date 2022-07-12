package de.kfzteile24.salesOrderHub.services.migration.exception;

public class NoProcessDefinitionFound extends RuntimeException {

    public NoProcessDefinitionFound(String message) {
        super(message);
    }
}
