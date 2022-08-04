package de.kfzteile24.salesOrderHub.controller.handler.impl;

import de.kfzteile24.salesOrderHub.controller.dto.ActionType;
import de.kfzteile24.salesOrderHub.controller.handler.AbstractActionHandler;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
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
            var returnOrder = salesOrderReturnService.getByOrderNumber(orderNumber);
            snsPublishService.publishCreditNoteReceivedEvent(
                    creditNoteEventMapper.toSalesCreditNoteReceivedEvent(returnOrder.getSalesCreditNoteCreatedMessage()));
        };
    }

    @Override
    public boolean supports(ActionType orderType) {
        return orderType == ActionType.REPUBLISH_CREDIT_NOTE;
    }
}
