package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_ORDER_ID;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;

@Slf4j
@Component
@RequiredArgsConstructor
public class DropshipmentOrderStoreInvoiceDelegate extends CommonDelegate {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final InvoiceService invoiceService;

    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var invoiceNumber = (String) delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName());
        var orderNumber = dropshipmentInvoiceRowService.getOrderNumberByInvoiceNumber(invoiceNumber);
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        salesOrder.setInvoiceEvent(invoiceService.generateInvoiceMessage(salesOrder));
        var updatedSalesOrder = salesOrderService.save(salesOrder, DROPSHIPMENT_INVOICE_STORED);

        // Sales Order ID is added as a variable for DropshipmentOrderPublishInvoiceDataDelegate
        delegateExecution.setVariable(ORDER_NUMBER.getName(), updatedSalesOrder.getOrderNumber());
        delegateExecution.setVariable(SALES_ORDER_ID.getName(), updatedSalesOrder.getId());
    }
}
