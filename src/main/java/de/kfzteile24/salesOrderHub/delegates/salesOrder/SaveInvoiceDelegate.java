package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderInvoice;
import de.kfzteile24.salesOrderHub.domain.converter.InvoiceSource;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
class SaveInvoiceDelegate implements JavaDelegate {

    private final SalesOrderService salesOrderService;
    private final InvoiceService invoiceService;

    private final DropshipmentOrderService dropshipmentOrderService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var invoiceUrl = (String) delegateExecution.getVariable(Variables.INVOICE_URL.getName());

        var salesOrderInvoice = createAndSaveSalesOrderInvoice(orderNumber, invoiceUrl);

        salesOrderService.getOrderByOrderNumber(orderNumber)
                .ifPresent(salesOrder -> invoiceService.addSalesOrderToInvoice(salesOrder, salesOrderInvoice));
    }

    private SalesOrderInvoice createAndSaveSalesOrderInvoice(String orderNumber, String invoiceUrl) {

        final var invoiceNumber = InvoiceUrlExtractor.extractInvoiceNumber(invoiceUrl);

        var invoiceSource = dropshipmentOrderService.isDropShipmentOrder(orderNumber) ? InvoiceSource.SOH : null;

        var salesOrderInvoice = SalesOrderInvoice.builder()
                .orderNumber(orderNumber)
                .invoiceNumber(invoiceNumber)
                .url(invoiceUrl)
                .source(invoiceSource)
                .build();

        return invoiceService.saveInvoice(salesOrderInvoice);
    }
}
