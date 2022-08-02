package de.kfzteile24.salesOrderHub.controller.handler.exception;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;

public class NoActionHandlerFoundException extends RuntimeException {

    public NoActionHandlerFoundException(ActionType actionType) {
        super("No action handler found for action type: " + actionType);
    }
}
