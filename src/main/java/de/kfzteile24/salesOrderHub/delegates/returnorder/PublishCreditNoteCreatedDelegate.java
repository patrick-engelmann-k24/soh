package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.events.SalesCreditNoteCreatedEvent;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.services.CreditNoteService;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
class PublishCreditNoteCreatedDelegate implements JavaDelegate {

    @Autowired
    private SnsPublishService snsPublishService;

    @Autowired
    private CreditNoteService creditNoteService;

    @Autowired
    private SalesOrderReturnService salesOrderReturnService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        var salesCreditNoteCreatedEvent = buildSalesCreditNoteCreatedEvent(delegateExecution);
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        SalesOrderReturn salesOrderReturn = Optional.of(salesOrderReturnService.getByOrderNumber(orderNumber))
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(orderNumber));
        var creditNoteNumber = salesOrderReturn.getSalesCreditNoteCreatedMessage() != null
                ? salesOrderReturn.getSalesCreditNoteCreatedMessage().getSalesCreditNote().getSalesCreditNoteHeader().getCreditNoteNumber()
                : "";
        snsPublishService.publishCreditNoteCreatedEvent(salesCreditNoteCreatedEvent);
        log.info("{} delegate invoked with order number: {} and credit note number: {}",
                PublishCreditNoteCreatedDelegate.class.getSimpleName(),
                orderNumber, creditNoteNumber);
    }

    SalesCreditNoteCreatedEvent buildSalesCreditNoteCreatedEvent(DelegateExecution delegateExecution) {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var creditNoteDocumentLink = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());
        return creditNoteService.buildSalesCreditNoteCreatedEvent(orderNumber, creditNoteDocumentLink);
    }
}
