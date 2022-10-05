package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChangeInvoiceAddressPossibleDelegate extends CommonDelegate {

    @NonNull
    private final InvoiceService invoiceService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final boolean invoiceExists = invoiceService.checkInvoiceExistsForOrder(orderNumber);

        setResultVariable(delegateExecution, Variables.INVOICE_EXISTS, invoiceExists);
    }
}
