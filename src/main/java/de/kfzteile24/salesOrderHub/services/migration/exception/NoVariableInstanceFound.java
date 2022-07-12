package de.kfzteile24.salesOrderHub.services.migration.exception;

public class NoVariableInstanceFound extends RuntimeException {

    public NoVariableInstanceFound(String processVariableName) {
        super(processVariableName);
    }
}
