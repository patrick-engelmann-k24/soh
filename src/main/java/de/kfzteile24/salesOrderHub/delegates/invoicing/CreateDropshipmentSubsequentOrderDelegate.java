package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.CommonDelegate;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentInvoiceRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED;
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
    @NonNull
    private final DropshipmentOrderRowService dropshipmentOrderRowService;
    @NonNull
    private final MetricsHelper metricsHelper;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("{} delegate invoked", CreateDropshipmentSubsequentOrderDelegate.class.getSimpleName());

        final var invoiceNumber = (String) delegateExecution.getVariable(Variables.INVOICE_NUMBER.getName());
        val invoiceData = dropshipmentInvoiceRowService.getInvoiceData(invoiceNumber);
        val orderNumber = invoiceData.getOrderNumber();
        val skuList = invoiceData.getOrderRows();
        val skuQuantityMap = invoiceData.getSkuQuantityMap();
        val salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));

        SalesOrder subsequentOrder = dropshipmentOrderService.createDropshipmentSubsequentSalesOrder(
                salesOrder,
                skuQuantityMap,
                invoiceNumber,
                delegateExecution.getActivityInstanceId());

        metricsHelper.sendCustomEventForDropshipmentOrder(subsequentOrder, DROPSHIPMENT_SUBSEQUENT_ORDER_CREATED);

        delegateExecution.setVariable(SUBSEQUENT_ORDER_NUMBER.getName(), subsequentOrder.getOrderNumber());
        delegateExecution.setVariable(ORDER_NUMBER.getName(), orderNumber);
        delegateExecution.setVariable(SALES_ORDER_ID.getName(), subsequentOrder.getId());
        prepareOrderRowCancellation(delegateExecution, orderNumber, skuList);
        log.info("Dropshipment subsequent order is created. Order Number: {}, Invoice Number: {}",
                orderNumber, invoiceNumber);
    }

    private void prepareOrderRowCancellation(DelegateExecution delegateExecution,
                                             String orderNumber, List<String> skuList) {
        delegateExecution.setVariable(
                ORDER_ROWS.getName(),
                dropshipmentOrderRowService.getSkuListToBeCancelled(orderNumber, skuList));
    }
}
