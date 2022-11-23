package de.kfzteile24.salesOrderHub.delegates.invoicing.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@Slf4j
@RequiredArgsConstructor
public class IsPartialInvoiceListener implements ExecutionListener {
    @NonNull
    private final SalesOrderService salesOrderService;
    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {

        final var invoiceNumber = (String) delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName());
        var invoiceData = dropshipmentInvoiceRowService.getInvoiceData(invoiceNumber);
        var isPartialInvoice =
                !salesOrderService.isFullyMatched(invoiceData.getOrderRows(), invoiceData.getOrderNumber());

        delegateExecution.setVariable(Variables.IS_PARTIAL_INVOICE.getName(), isPartialInvoice);
        delegateExecution.setVariable(Variables.ORDER_ROWS.getName(), new ArrayList<>());
    }
}