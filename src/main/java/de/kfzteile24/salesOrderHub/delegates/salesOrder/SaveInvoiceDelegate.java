package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.export.AmazonS3Service;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SaveInvoiceDelegate implements JavaDelegate {

    private final SalesOrderService salesOrderService;
    private final InvoiceService invoiceService;
    private final AmazonS3Service amazonS3Service;
    private final DropshipmentOrderService dropshipmentOrderService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {

        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());
        final var invoiceNumber = InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl);

        var isDuplicateDropshipmentInvoice = false;
        var optionalSalesOrderInvoice = invoiceService.getSalesOrderInvoiceByInvoiceNumber(invoiceNumber);
        SalesOrderInvoice salesOrderInvoice;

        if (optionalSalesOrderInvoice.isPresent()) {
            isDuplicateDropshipmentInvoice = true;
            salesOrderInvoice = optionalSalesOrderInvoice.get();
            final var oldInvoicePath = salesOrderInvoice.getUrl();
            salesOrderInvoice.setUrl(invoiceUrl);
            amazonS3Service.deleteFile(oldInvoicePath);
        } else {
            salesOrderInvoice = createSalesOrderInvoice(orderNumber, invoiceUrl);
        }

        saveSalesOrderInvoice(salesOrderInvoice);
        delegateExecution.setVariable(Variables.IS_DUPLICATE_DROPSHIPMENT_INVOICE.getName(), isDuplicateDropshipmentInvoice);
    }

    private SalesOrderInvoice createSalesOrderInvoice(String orderNumber, String invoiceUrl) {

        final var invoiceNumber = InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl);
        var invoiceSource = dropshipmentOrderService.isDropShipmentOrder(orderNumber) ? InvoiceSource.SOH : null;

        return SalesOrderInvoice.builder()
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .url(invoiceUrl)
                .source(invoiceSource)
                .build();
    }

    private void saveSalesOrderInvoice(SalesOrderInvoice salesOrderInvoice) {

        invoiceService.saveInvoice(salesOrderInvoice);
        salesOrderService.getOrderByOrderNumber(salesOrderInvoice.getOrderNumber())
                .ifPresent(salesOrder -> invoiceService.addSalesOrderToInvoice(salesOrder, salesOrderInvoice));

    }
}
