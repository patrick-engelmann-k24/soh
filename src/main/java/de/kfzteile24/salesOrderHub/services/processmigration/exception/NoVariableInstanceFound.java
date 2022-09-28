package de.kfzteile24.salesOrderHub.services.processmigration.exception;

public class NoVariableInstanceFound extends RuntimeException {

    public NoVariableInstanceFound(String processVariableName) {
        super(processVariableName);
    }
}
