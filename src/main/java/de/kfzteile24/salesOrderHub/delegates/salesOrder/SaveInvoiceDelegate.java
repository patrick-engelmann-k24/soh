package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.services.InvoiceService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;

@Component
@Slf4j
@RequiredArgsConstructor
public class SaveInvoiceDelegate implements JavaDelegate {
    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final InvoiceService invoiceService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());
        final var invoiceNumber = extractInvoiceNumber(invoiceUrl);

        final var salesOrderOpt = salesOrderService.getOrderByOrderNumber(orderNumber);

        var invoice = SalesOrderInvoice.builder()
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .url(invoiceUrl)
                .build();
        invoice = invoiceService.saveInvoice(invoice);

        if (salesOrderOpt.isPresent()) {
            final var salesOrder = salesOrderOpt.get();
            invoice.setSalesOrder(salesOrder);
            salesOrder.getSalesOrderInvoiceList().add(invoice);
            salesOrderService.save(salesOrder, INVOICE_RECEIVED);
        }
    }

    private String extractInvoiceNumber(final String invoiceUrl) {
        final var afterLastMinus = invoiceUrl.lastIndexOf('-') + 1;

        if (afterLastMinus > 0) {
            final var dot = invoiceUrl.indexOf('.', afterLastMinus);
            if (dot != -1) {
                return invoiceUrl.substring(afterLastMinus, dot);
            }
        }

        throw new IllegalArgumentException("Cannot parse InvoiceNumber from invoice url: " + invoiceUrl);
    }
}
