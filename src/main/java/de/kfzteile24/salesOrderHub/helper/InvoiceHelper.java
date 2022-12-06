package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceHelper {

    private final InvoiceService invoiceService;

    public void setSalesOrderInvoice(SalesOrder salesOrder, String invoiceNumber) {
        salesOrder.getLatestJson().getOrderHeader().setDocumentRefNumber(invoiceNumber);
        var invoiceMessage = invoiceService.generateInvoiceMessage(salesOrder);
        invoiceMessage.getSalesInvoice().getSalesInvoiceHeader().setOrderGroupId(
                salesOrder.getLatestJson().getOrderHeader().getOrderGroupId());
        salesOrder.setInvoiceEvent(invoiceMessage);
    }
}
