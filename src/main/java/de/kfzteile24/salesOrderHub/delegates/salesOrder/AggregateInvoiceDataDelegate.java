package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.bpmn.orderProcess.InvoiceDataVariable;
import de.kfzteile24.salesOrderHub.domain.dropshipment.DropshipmentInvoiceRow;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import de.kfzteile24.salesOrderHub.services.financialdocuments.InvoiceService;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
        log.info("Aggregate invoice data");

        //process
        //salesOrderService.cancelOrder(orderNumber);

        //1. Fetch all data from ‘dropshipment_invoice_row’ table by ordering according to order number.
        var dropshipmentInvoiceRows = dropshipmentInvoiceRowService.findAllOrderByOrderNumberAsc();

        //Put row data into a map of which order numbers are the key values
        //
        //Loop all data based on order number (map data)
        var invoiceDataMap = generateInvoiceDataMap(dropshipmentInvoiceRows);

        //For each order number
        //
        Collection<InvoiceDataVariable> invoiceDataVariableList = new ArrayList<>();
        for (var orderNumber : invoiceDataMap.keySet()) {

            //Create an invoice number save it to the invoice number field of the table for each of the order row entities related with that order number
            var invoiceNumber = invoiceService.createInvoiceNumber();
            dropshipmentInvoiceRowService.saveInvoiceNumber(orderNumber, invoiceNumber);

            //Check if the row list is partly covered or not and invoiced flag is set
            // source: skuList (value in map)
            // target: original dropshipment order sku list fetched by orderNumber (key in map)
            var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                    .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
            var salesOrderSkuList = salesOrder.getLatestJson().getOrderRows().stream().map(OrderRows::getSku).collect(Collectors.toList());

            boolean isPartialInvoice = invoiceDataMap.get(orderNumber).stream().allMatch(sku -> salesOrderSkuList.contains(sku));

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