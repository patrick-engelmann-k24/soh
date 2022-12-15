package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DetermineDropshipmentOrderInvoiceTypeDelegate implements JavaDelegate {

    private final SalesOrderService salesOrderService;
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("{} delegate invoked", DetermineDropshipmentOrderInvoiceTypeDelegate.class.getSimpleName());

        final var invoiceNumber = (String) delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName());
        var invoiceData = dropshipmentInvoiceRowService.getInvoiceData(invoiceNumber);
        var isPartialInvoice =
                !salesOrderService.isFullyMatched(invoiceData.getOrderRows(), invoiceData.getOrderNumber());

        delegateExecution.setVariable(Variables.IS_PARTIAL_INVOICE.getName(), isPartialInvoice);
    }
}
