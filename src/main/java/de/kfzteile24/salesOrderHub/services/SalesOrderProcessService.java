package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SALES_ORDER_CONSUMED;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SPLIT_ORDER_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderProcessService {

    private final SalesOrderService salesOrderService;
    private final CamundaHelper camundaHelper;
    private final SplitterService splitterService;
    private final MetricsHelper metricsHelper;
    private final SnsPublishService snsPublishService;
    private final OrderUtil orderUtil;

    @EnrichMessageForDlq
    public void handleShopOrdersReceived(Order order, MessageWrapper messageWrapper) {

        log.info("Received shop order message with order number: {}. Platform: {}. Receive count: {}. Sender ID: {}",
                order.getOrderHeader().getOrderNumber(),
                order.getOrderHeader().getPlatform(),
                messageWrapper.getReceiveCount(),
                messageWrapper.getSenderId());

        salesOrderService.enrichInitialOrder(order);
        var orderCopy = orderUtil.copyOrderJson(order);
        splitterService.splitSalesOrder(order, orderCopy).forEach(this::startSalesOrderProcess);
    }

    public void startSalesOrderProcess(SalesOrderSplit salesOrderSplit) {
        var salesOrder = salesOrderSplit.getOrder();
        if (salesOrderService.checkOrderNotExists(salesOrder.getOrderNumber())) {
            SalesOrder createdSalesOrder = salesOrderService.createSalesOrder(salesOrder);
            Order order = createdSalesOrder.getLatestJson();
            if (orderUtil.checkIfOrderHasOrderRows(order)) {
                ProcessInstance result = camundaHelper.createOrderProcess(createdSalesOrder, ORDER_RECEIVED_ECP);

                if (result != null) {
                    log.info("New ecp order process started for order number: {}. Process-Instance-ID: {} ",
                            salesOrder.getOrderNumber(), result.getProcessInstanceId());
                    metricsHelper.sendCustomEvent(salesOrder, SALES_ORDER_CONSUMED);
                    if (salesOrderSplit.isSplitted()) {
                        metricsHelper.sendCustomEvent(salesOrder, SPLIT_ORDER_GENERATED);
                    }
                }
            } else {
                snsPublishService.publishOrderCreated(createdSalesOrder.getOrderNumber());
            }
        }
    }
}
