package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashSet;

@Component
@Log
@RequiredArgsConstructor
public class SaveInvoiceDelegate implements JavaDelegate {
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());
        final var invoiceNumber = extractInvoiceNumber(invoiceUrl);

        var salesOrderOptional = salesOrderService.getOrderByOrderNumber(orderNumber);

        if (salesOrderOptional.isPresent()) {
            final SalesOrder salesOrder = salesOrderOptional.get();
            final var invoice = SalesOrderInvoice.builder()
                    .salesOrder(salesOrder)
                    .invoiceNumber(invoiceNumber)
                    .url(invoiceUrl)
                    .build();
            if (salesOrder.getSalesOrderInvoiceList() == null) {
                salesOrder.setSalesOrderInvoiceList(new HashSet<>());
            }
            salesOrder.getSalesOrderInvoiceList().add(invoice);
            salesOrderService.save(salesOrder);
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
