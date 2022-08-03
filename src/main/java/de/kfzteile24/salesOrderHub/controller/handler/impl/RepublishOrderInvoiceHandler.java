package de.kfzteile24.salesOrderHub.controller.handler.impl;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.handler.AbstractActionHandler;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RepublishOrderInvoiceHandler extends AbstractActionHandler {

    private final SalesOrderService salesOrderService;

    @Override
    protected Consumer<String> getAction() {
        return orderNumber -> {
            var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
            snsPublishService.publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        };
    }

    @Override
    public boolean supports(ActionType orderType) {
        return orderType == ActionType.RECREATE_ORDER_INVOICE;
    }
}
