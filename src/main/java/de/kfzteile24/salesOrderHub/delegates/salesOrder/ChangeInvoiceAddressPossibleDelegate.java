package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Log
public class ChangeInvoiceAddressPossibleDelegate extends CommonDelegate {

    @Autowired
    SalesOrderInvoiceRepository invoiceRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String orderId = (String) delegateExecution.getVariable(Variables.VAR_ORDER_NUMBER.getName());
        setResultVariable(delegateExecution, Variables.VAR_INVOICE_EXISTS, checkInvoiceExistentForOrder(orderId));
    }

    /**
     * If we find an invoice, there are already invoice(s) created
     * @param orderId
     * @return
     */
    Boolean checkInvoiceExistentForOrder(final String orderId) {
        final List<SalesOrderInvoice> orderInvoiceList = invoiceRepository.getInvoicesByOrderNumber(orderId);
        return orderInvoiceList.size() > 0;
    }
}
