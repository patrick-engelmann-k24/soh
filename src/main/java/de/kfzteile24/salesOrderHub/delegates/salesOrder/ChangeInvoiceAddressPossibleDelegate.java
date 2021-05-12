package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.repositories.SalesOrderInvoiceRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChangeInvoiceAddressPossibleDelegate extends CommonDelegate {

    @NonNull
    private final SalesOrderInvoiceRepository invoiceRepository;

    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        setResultVariable(delegateExecution, Variables.INVOICE_EXISTS, checkInvoiceExistentForOrder(orderNumber));
    }

    /**
     * If we find an invoice, there are already invoice(s) created
     *
     * @param orderNumber
     * @return
     */
    private Boolean checkInvoiceExistentForOrder(final String orderNumber) {
        final List<SalesOrderInvoice> orderInvoiceList = invoiceRepository.getInvoicesByOrderNumber(orderNumber);
        return orderInvoiceList.size() > 0;
    }
}
