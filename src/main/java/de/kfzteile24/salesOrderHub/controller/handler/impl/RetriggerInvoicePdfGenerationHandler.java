package de.kfzteile24.salesOrderHub.controller.handler.impl;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.handler.AbstractActionHandler;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RetriggerInvoicePdfGenerationHandler extends AbstractActionHandler {

    private final SalesOrderService salesOrderService;

    @Override
    protected Consumer<String> getAction() {
        return orderNumber -> {
            var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
            snsPublishService.publishInvoicePdfGenerationTriggeredEvent(salesOrder.getLatestJson());
        };
    }

    @Override
    public boolean supports(ActionType orderType) {
        return orderType == ActionType.RETRIGGER_INVOICE_PDF_GENERATION;
    }

}
