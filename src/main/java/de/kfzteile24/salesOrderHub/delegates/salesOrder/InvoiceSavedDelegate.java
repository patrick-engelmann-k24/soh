package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvoiceSavedDelegate implements JavaDelegate {

    private final SnsPublishService snsPublishService;
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());
        final var isDuplicateDropshipmentInvoice = (Boolean) delegateExecution.getVariable(Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName());

        if (isDropShipmentRelated(orderNumber) && !isDuplicateDropshipmentInvoice) {
            snsPublishService.publishOrderInvoiceCreated(orderNumber, invoiceUrl);
        }
    }

    private boolean isDropShipmentRelated(String orderNumber) {
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrder.isPresent()) {
            final var orderJson = (Order) salesOrder.get().getLatestJson();
            return InvoiceUrlExtractor.isDropShipmentRelated(orderJson.getOrderHeader().getOrderFulfillment());
        }
        return false;
    }
}