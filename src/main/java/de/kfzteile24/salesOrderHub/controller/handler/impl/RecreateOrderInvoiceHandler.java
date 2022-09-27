package de.kfzteile24.salesOrderHub.controller.handler.impl;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.handler.AbstractActionHandler;
import de.kfzteile24.salesOrderHub.helper.EventMapper;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static java.util.Collections.emptyList;

@Component
@RequiredArgsConstructor
public class RecreateOrderInvoiceHandler extends AbstractActionHandler {

    private final DropshipmentOrderService dropshipmentOrderService;

    @Override
    protected Consumer<String> getAction() {
        return orderNumber -> {
            var salesOrder = dropshipmentOrderService.recreateSalesOrderInvoice(orderNumber);
            snsPublishService.publishSalesOrderShipmentConfirmedEvent(salesOrder, emptyList());
            snsPublishService.publishCoreInvoiceReceivedEvent(EventMapper.INSTANCE.toCoreSalesInvoiceCreatedReceivedEvent(salesOrder.getInvoiceEvent()));
        };
    }

    @Override
    public boolean supports(ActionType orderType) {
        return orderType == ActionType.RECREATE_ORDER_INVOICE;
    }
}
