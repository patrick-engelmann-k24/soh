package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.bpmn.orderProcess.InvoiceDataVariable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IsPartialInvoiceDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {

        InvoiceDataVariable invoiceData =
                (InvoiceDataVariable) delegateExecution.getVariable(Variables.INVOICE_DATA.getName());

        var isPartialInvoice = invoiceData.isPartialInvoice();

        delegateExecution.setVariable(Variables.IS_PARTIAL_INVOICE.getName(), isPartialInvoice);

    }
}
