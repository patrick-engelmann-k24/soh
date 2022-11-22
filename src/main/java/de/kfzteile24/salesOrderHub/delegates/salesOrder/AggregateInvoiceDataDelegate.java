package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.bpmn.orderProcess.InvoiceDataVariable;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_DATA_LIST;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregateInvoiceDataDelegate implements JavaDelegate {

    private final CamundaHelper camundaHelper;
    @NonNull
    private final SalesOrderService salesOrderService;
    @NonNull
    private final InvoiceService invoiceService;
    @NonNull
    private final DropshipmentOrderService dropshipmentOrderService;
    @NonNull
    private final DropshipmentInvoiceRowService dropshipmentInvoiceRowService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("Daily aggregation of invoice data has started.");

        var invoiceDataMap =
                generateInvoiceDataMap(dropshipmentInvoiceRowService.findAllOrderByOrderNumberAsc());

        Collection<InvoiceDataVariable> invoiceDataVariableList = new ArrayList<>();
        for (var orderNumber : invoiceDataMap.keySet()) {

            var invoiceNumber = invoiceService.createInvoiceNumber();
            dropshipmentInvoiceRowService.saveInvoiceNumber(orderNumber, invoiceNumber);

            boolean isPartialInvoice = !salesOrderService.isFullyMatched(invoiceDataMap.get(orderNumber), orderNumber);

            InvoiceDataVariable invoiceDataVariable = new InvoiceDataVariable(invoiceNumber, isPartialInvoice);
            invoiceDataVariableList.add(invoiceDataVariable);
        }

        camundaHelper.setVariable(delegateExecution.getProcessInstanceId(), INVOICE_DATA_LIST.getName(), invoiceDataVariableList);

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