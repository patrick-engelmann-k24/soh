package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class InvoiceSavedDelegate implements JavaDelegate {

    private final SnsPublishService snsPublishService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());

        if (InvoiceUrlExtractor.isDropShipmentRelated(invoiceUrl)) {
            snsPublishService.publishOrderInvoiceCreated(orderNumber, invoiceUrl);
        }
    }
}