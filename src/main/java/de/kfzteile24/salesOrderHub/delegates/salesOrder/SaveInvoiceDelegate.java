package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.HashSet;

import static de.kfzteile24.salesOrderHub.domain.audit.Action.INVOICE_RECEIVED;

@Component
@Slf4j
@RequiredArgsConstructor
public class SaveInvoiceDelegate implements JavaDelegate {
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());
        final var invoiceNumber = extractInvoiceNumber(invoiceUrl);

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                                                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        final var invoice = SalesOrderInvoice.builder()
                                             .salesOrder(salesOrder)
                                             .invoiceNumber(invoiceNumber)
                                             .url(invoiceUrl)
                                             .build();

        if (salesOrder.getSalesOrderInvoiceList() == null) {
            salesOrder.setSalesOrderInvoiceList(new HashSet<>());
        }

        salesOrder.getSalesOrderInvoiceList().add(invoice);
        salesOrderService.save(salesOrder, INVOICE_RECEIVED);
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
