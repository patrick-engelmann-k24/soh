package de.kfzteile24.salesOrderHub.delegates.returnorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.dto.mapper.CreditNoteEventMapper;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PublishCreditNoteReceivedDelegate implements JavaDelegate {

    @Autowired
    private SnsPublishService snsPublishService;

    @Autowired
    private SalesOrderReturnService salesOrderReturnService;

    @Autowired
    private CreditNoteEventMapper creditNoteEventMapper;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        SalesOrderReturn salesOrderReturn = salesOrderReturnService.getByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(orderNumber));
        var salesCreditNoteCreatedMessage = salesOrderReturn.getSalesCreditNoteCreatedMessage();
        var salesCreditNoteReceivedEvent =
                creditNoteEventMapper.toSalesCreditNoteReceivedEvent(salesCreditNoteCreatedMessage);

        log.info("{} delegate invoked", PublishCreditNoteReceivedDelegate.class.getSimpleName());
        if (InvoiceUrlExtractor.isDropShipmentRelated(salesOrderReturn.getReturnOrderJson().getOrderHeader().getOrderFulfillment())) {
            snsPublishService.publishCreditNoteCreatedEvent(salesCreditNoteReceivedEvent);
            log.info("Dropshipment Credit Note Event is published from soh-credit-note-created-v1");
        } else {
            snsPublishService.publishCreditNoteReceivedEvent(salesCreditNoteReceivedEvent);
            log.info("Core Sales Credit Note Event is published from soh-credit-note-received-v1");
        }
    }
}
