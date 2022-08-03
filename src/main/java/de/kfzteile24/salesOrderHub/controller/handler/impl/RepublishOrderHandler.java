package de.kfzteile24.salesOrderHub.controller.handler.impl;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.handler.AbstractActionHandler;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class RepublishOrderHandler extends AbstractActionHandler {

    @Override
    protected Consumer<String> getAction() {
        return snsPublishService::publishMigrationOrderCreated;
    }

    @Override
    public boolean supports(ActionType orderType) {
        return orderType == ActionType.REPUBLISH_ORDER;
    }
}
