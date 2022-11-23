package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.kfzteile24.salesOrderHub.domain.bpmn.orderProcess.InvoiceDataVariable;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_DATA_LIST;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregateInvoiceDataListener implements ExecutionListener {

    @NonNull
    private final SalesOrderService salesOrderService;
    @NonNull
    private final InvoiceService invoiceService;
    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;
    @NonNull
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void notify(DelegateExecution delegateExecution) throws Exception {
        log.info("Daily aggregation of invoice data has started.");

        var invoiceDataMap =
                generateInvoiceDataMap(dropshipmentInvoiceRowService.findAllOrderByOrderNumberAsc());

        List<String> invoiceDataVariableList = new ArrayList<>();
        for (var orderNumber : invoiceDataMap.keySet()) {

            var invoiceNumber = invoiceService.createInvoiceNumber();
            dropshipmentInvoiceRowService.saveInvoiceNumber(orderNumber, invoiceNumber);

            boolean isPartialInvoice = !salesOrderService.isFullyMatched(invoiceDataMap.get(orderNumber), orderNumber);

            InvoiceDataVariable invoiceDataVariable = new InvoiceDataVariable(invoiceNumber, orderNumber, isPartialInvoice);
            invoiceDataVariableList.add(objectMapper.writeValueAsString(invoiceDataVariable));
        }

        delegateExecution.setVariable(INVOICE_DATA_LIST.getName(), invoiceDataVariableList);

    }

    public Map<String, List<String>> generateInvoiceDataMap(Collection<DropshipmentInvoiceRow> dropshipmentInvoiceRows) {
        Map<String, List<String>> dropshipmentInvoiceRowMap = new HashMap<>();
        dropshipmentInvoiceRows.forEach(item -> {
            var key = item.getOrderNumber();
            var value = item.getSku();
            var valueList = dropshipmentInvoiceRowMap.get(key);
            if (valueList == null) {
                valueList = new ArrayList<>(List.of(value));
            } else {
                valueList.add(value);
            }
            dropshipmentInvoiceRowMap.put(key, valueList);
        });
        return dropshipmentInvoiceRowMap;
    }
}