package de.kfzteile24.salesOrderHub.services.dropshipment;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.InvoiceUrlExtractor;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.INVOICE_URL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Service
@RequiredArgsConstructor
@Slf4j
public class DropshipmentOrderReturnService {
    private final SalesOrderReturnService salesOrderReturnService;
    private final RuntimeService runtimeService;
    private final MetricsHelper metricsHelper;

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

        var salesOrder = salesOrderReturnService.getOrderByReturnOrderNumber(returnOrderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order by return order number: " + returnOrderNumber));
        metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, DROPSHIPMENT_ORDER_CREDIT_NOTE_CREATED);
        log.info("Invoice {} for credit note of dropshipment order return for order-number {} successfully received",
                invoiceUrl, returnOrderNumber);
    }
}
