package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDropshipmentSubsequentInvoiceDelegate implements JavaDelegate {
    @NonNull
    private final SalesOrderService salesOrderService;
    @NonNull
    private final InvoiceService invoiceService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("{} delegate invoked", CreateDropshipmentSubsequentInvoiceDelegate.class.getSimpleName());

        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        var invoice = invoiceService.generateInvoiceMessage(salesOrder);
        salesOrder.setInvoiceEvent(invoice);
        salesOrderService.save(salesOrder, DROPSHIPMENT_INVOICE_STORED);
        log.info("Dropshipment partial invoice is created. Order Number: {}, Invoice Number: {}",
                orderNumber, invoice.getSalesInvoice().getSalesInvoiceHeader().getInvoiceNumber());
    }
}
