package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_ORDER_ID;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SUBSEQUENT_ORDER_NUMBER;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDropshipmentSubsequentOrderDelegate extends CommonDelegate {
    @NonNull
    private final SalesOrderService salesOrderService;
    @NonNull
    private final DropshipmentOrderService dropshipmentOrderService;
    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("{} delegate invoked", CreateDropshipmentSubsequentOrderDelegate.class.getSimpleName());

        final var invoiceNumber = (String) delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName());
        var invoiceData = dropshipmentInvoiceRowService.getInvoiceData(invoiceNumber);
        var orderNumber = invoiceData.getOrderNumber();
        var skuList = invoiceData.getOrderRows();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        SalesOrder subsequentOrder = dropshipmentOrderService.createDropshipmentSubsequentSalesOrder(
                salesOrder,
                skuList,
                invoiceNumber,
                delegateExecution.getActivityInstanceId());

        delegateExecution.setVariable(SUBSEQUENT_ORDER_NUMBER.getName(), subsequentOrder.getOrderNumber());
        delegateExecution.setVariable(ORDER_NUMBER.getName(), orderNumber);
        delegateExecution.setVariable(SALES_ORDER_ID.getName(), subsequentOrder.getId());
        delegateExecution.setVariable(ORDER_ROWS.getName(), skuList);
        log.info("Dropshipment subsequent order is created. Order Number: {}, Invoice Number: {}",
                orderNumber, invoiceNumber);
    }
}
