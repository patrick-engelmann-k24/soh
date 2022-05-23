package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.InvoiceService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_ORDER_ID;
import static de.kfzteile24.salesOrderHub.domain.audit.Action.DROPSHIPMENT_INVOICE_STORED;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreDropshipmentInvoiceDelegate extends CommonDelegate {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final InvoiceService invoiceService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());

        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        salesOrder.setInvoiceEvent(invoiceService.generateInvoiceMessage(salesOrder));
        var updatedSalesOrder = salesOrderService.save(salesOrder, DROPSHIPMENT_INVOICE_STORED);

        // Sales Order ID is added as a variable for PublishCoreSalesInvoiceCreatedReceivedDelegate
        delegateExecution.setVariable(SALES_ORDER_ID.getName(), updatedSalesOrder.getId());
    }
}
