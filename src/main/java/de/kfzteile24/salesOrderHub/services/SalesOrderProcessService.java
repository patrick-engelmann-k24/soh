package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalesOrderProcessService {

    private final SalesOrderService salesOrderService;
    private final CamundaHelper camundaHelper;
    private final SplitterService splitterService;

    private final MetricsHelper metricsHelper;

    public void handleShopOrdersReceived(MessageWrapper<Order> orderMessageWrapper) {
        setOrderGroupIdIfEmpty(orderMessageWrapper.getMessage());
        splitterService.splitSalesOrder(orderMessageWrapper.getMessage())
                .forEach(salesOrderSplit -> startSalesOrderProcess(salesOrderSplit, orderMessageWrapper));
    }

    public void startSalesOrderProcess(SalesOrderSplit salesOrderSplit, MessageWrapper<Order> orderMessageWrapper) {
        var salesOrder = salesOrderSplit.getOrder();
        try {
            if (salesOrderService.checkOrderNotExists(salesOrder.getOrderNumber())) {
                ProcessInstance result = camundaHelper.createOrderProcess(
                        salesOrderService.createSalesOrder(salesOrder), ORDER_RECEIVED_ECP);

                if (result != null) {
                    log.info("New ecp order process started for order number: {}. Process-Instance-ID: {} ",
                            salesOrder.getOrderNumber(), result.getProcessInstanceId());
                    metricsHelper.salesOrderConsumed(salesOrder);
                    if (salesOrderSplit.isSplitted()) {
                        metricsHelper.splittedOrderGenerated(salesOrder);
                    }
                }
            }
        } catch (Exception e) {
            log.error("New ecp order process is failed by message error:\r\nError-Message: {}, Message Body: {}",
                    e.getMessage(), orderMessageWrapper.getSqsMessage().getBody());
            throw e;
        }
    }

    private void setOrderGroupIdIfEmpty(Order order) {
        String orderNumber = order.getOrderHeader().getOrderNumber();
        if (StringUtils.isBlank(order.getOrderHeader().getOrderGroupId())) {
            order.getOrderHeader().setOrderGroupId(orderNumber);
        }
    }
}
