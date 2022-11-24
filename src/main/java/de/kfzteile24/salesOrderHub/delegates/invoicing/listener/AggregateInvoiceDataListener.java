package de.kfzteile24.salesOrderHub.delegates.invoicing.listener;

import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_NUMBER_LIST;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregateInvoiceDataListener implements ExecutionListener {
    @NonNull
    private final InvoiceService invoiceService;
    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    @Transactional
    public void notify(DelegateExecution delegateExecution) throws Exception {
        log.info("Daily aggregation of invoice data has started.");

        var invoiceDataMap = dropshipmentInvoiceRowService.generateInvoiceDataMap(
                dropshipmentInvoiceRowService.findAllOrderByOrderNumberAsc());

        List<String> invoiceNumberList = new ArrayList<>();
        for (var orderNumber : invoiceDataMap.keySet()) {

            var invoiceNumber = invoiceService.createInvoiceNumber();
            dropshipmentInvoiceRowService.saveInvoiceNumber(orderNumber, invoiceNumber);
            invoiceNumberList.add(invoiceNumber);
        }

        delegateExecution.setVariable(INVOICE_NUMBER_LIST.getName(), invoiceNumberList);
    }
}