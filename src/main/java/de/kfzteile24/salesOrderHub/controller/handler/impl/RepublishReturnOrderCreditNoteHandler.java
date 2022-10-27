package de.kfzteile24.salesOrderHub.controller.handler.impl;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.handler.AbstractActionHandler;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteReceivedEvent;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class RepublishReturnOrderCreditNoteHandler extends AbstractActionHandler {

    private final SalesOrderReturnService salesOrderReturnService;
    private final CreditNoteEventMapper creditNoteEventMapper;

    @Override
    protected Consumer<String> getAction() {
        return orderNumber -> {
            var returnOrder = salesOrderReturnService.getByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderReturnNotFoundException(orderNumber));
            SalesCreditNoteReceivedEvent creditNoteEvent =
                    creditNoteEventMapper.toSalesCreditNoteReceivedEvent(returnOrder.getSalesCreditNoteCreatedMessage());

            if (InvoiceUrlExtractor.isDropShipmentRelated(returnOrder.getReturnOrderJson().getOrderHeader().getOrderFulfillment())) {
                snsPublishService.publishCreditNoteCreatedEvent(creditNoteEvent);
            } else {
                snsPublishService.publishCreditNoteReceivedEvent(creditNoteEvent);
            }
        };
    }

    @Override
    public boolean supports(ActionType orderType) {
        return orderType == ActionType.REPUBLISH_CREDIT_NOTE;
    }
}
