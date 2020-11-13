package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.services.InvoiceService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log
public class ChangeInvoiceAddressPossibleDelegate extends CommonDelegate {

    @Autowired
    InvoiceService invoiceService;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        setResultVariable(delegateExecution, Variables.INVOICE_EXISTS, invoiceService.checkInvoiceExistentForOrder(orderNumber));
    }
}
