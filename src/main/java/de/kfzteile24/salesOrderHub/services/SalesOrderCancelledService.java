package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.CORE_SALES_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderCancelledService {

    private final SalesOrderService salesOrderService;
    private final CamundaHelper camundaHelper;

    @EnrichMessageForDlq
    public void handleCoreSalesOrderCancelled(CoreSalesOrderCancelledMessage message, MessageWrapper messageWrapper) {
        final var orderNumber = message.getOrderNumber();
        log.info("Received core sales order cancelled message with order number: {}", orderNumber);
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        camundaHelper.correlateMessage(CORE_SALES_ORDER_CANCELLED, salesOrder,
                Variables.putValue(ORDER_NUMBER.getName(), orderNumber)); // start event subprocess interrupting the main process at any point
    }
}
