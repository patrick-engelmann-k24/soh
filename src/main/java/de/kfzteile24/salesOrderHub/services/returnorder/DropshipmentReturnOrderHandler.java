package de.kfzteile24.salesOrderHub.services.returnorder;

import de.kfzteile24.salesOrderHub.constants.CustomEventName;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.exception.SalesOrderReturnNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Component
@RequiredArgsConstructor
@Slf4j
public class DropshipmentReturnOrderHandler implements ReturnOrderCreator {

    private final SalesOrderService salesOrderService;
    private final SalesOrderReturnService salesOrderReturnService;
    private final OrderUtil orderUtil;
    private final RuntimeService runtimeService;
    private final MetricsHelper metricsHelper;

    @Override
    public List<SalesOrder> getSalesOrderList(String orderGroupId) {
        return salesOrderService.getOrderByOrderGroupId(orderGroupId)
                .stream()
                .filter(order -> !order.isCancelled())
                .filter(order -> orderUtil.isDropshipmentOrder(order.getLatestJson()))
                .sorted(Comparator.comparing(SalesOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @EnrichMessageForDlq
    public void handleCreditNoteFromDropshipmentOrderReturn(String invoiceUrl, MessageWrapper messageWrapper) {

        final var returnOrderNumber = InvoiceUrlExtractor.extractReturnOrderNumber(invoiceUrl);
        log.info("Received credit note from dropshipment with return order number: {} ", returnOrderNumber);

        String message = Messages.DROPSHIPMENT_CREDIT_NOTE_DOCUMENT_GENERATED.getName();
        final Map<String, Object> processVariables = Map.of(
                ORDER_NUMBER.getName(), returnOrderNumber,
                INVOICE_URL.getName(), invoiceUrl
        );

        runtimeService.startProcessInstanceByMessage(message, returnOrderNumber, processVariables);
        var returnOrder = salesOrderReturnService.getByOrderNumber(returnOrderNumber)
                .orElseThrow(() -> new SalesOrderReturnNotFoundException(returnOrderNumber));
        var salesOrder = returnOrder.getSalesOrder();
        metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED);
        log.info("Invoice {} for credit note of dropshipment order return for order-number {} successfully received",
                invoiceUrl, returnOrderNumber);
    }
}
