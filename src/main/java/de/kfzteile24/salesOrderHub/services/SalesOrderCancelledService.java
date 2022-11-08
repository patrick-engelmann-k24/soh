package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.sns.CoreSalesOrderCancelledMessage;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Service;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_CANCELLATION_RECEIVED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderCancelledService {

    private final SalesOrderService salesOrderService;
    private final SnsPublishService snsPublishService;
    private final CamundaHelper camundaHelper;

    private final SalesOrderRowService salesOrderRowService;

    @EnrichMessageForDlq
    public void handleCoreSalesOrderCancelled(CoreSalesOrderCancelledMessage message, MessageWrapper messageWrapper) {
        final var orderNumber = message.getOrderNumber();
        log.info("Received core sales order cancelled message with order number: {}", orderNumber);
        SalesOrder salesOrder = null; // get sales order from DB
        camundaHelper.correlateMessage(ORDER_CANCELLATION_RECEIVED, salesOrder,
                Variables.putValue(ORDER_NUMBER.getName(), orderNumber)); // start event subprocess interrupting the main process at any point
        // var salesOrder = salesOrderRowService.cancelOrder(orderNumber); goes to the new delegate to be created
        // snsPublishService.publishOrderCancelled(salesOrder.getLatestJson()); not required -> see OrderCancelledDelegate
    }
}
