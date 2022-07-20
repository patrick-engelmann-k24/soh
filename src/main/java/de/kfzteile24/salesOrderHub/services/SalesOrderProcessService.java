package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    public void handleShopOrdersReceived(MessageWrapper<Order> orderMessageWrapper) {
        setOrderGroupIdIfEmpty(orderMessageWrapper.getMessage());
        splitterService.splitSalesOrder(orderMessageWrapper.getMessage())
                .forEach(salesOrderSplit -> startSalesOrderProcess(salesOrderSplit, orderMessageWrapper));
    }

    public void startSalesOrderProcess(SalesOrderSplit salesOrderSplit, MessageWrapper<Order> orderMessageWrapper) {
        var salesOrder = salesOrderSplit.getOrder();
        try {
            if (salesOrderService.checkOrderNotExists(salesOrder.getOrderNumber())) {
                SalesOrder createdSalesOrder = salesOrderService.createSalesOrder(salesOrder);
                Order order = createdSalesOrder.getLatestJson();
                if (order.getOrderRows() == null || order.getOrderRows().isEmpty()) {
                    log.info("Sales order with order number {} has no order rows. Camunda process is not created!",
                            createdSalesOrder.getOrderNumber());
                    snsPublishService.publishOrderCreated(createdSalesOrder.getOrderNumber());
                } else {
                    ProcessInstance result = camundaHelper.createOrderProcess(createdSalesOrder, ORDER_RECEIVED_ECP);

                    if (result != null) {
                        log.info("New ecp order process started for order number: {}. Process-Instance-ID: {} ",
                                salesOrder.getOrderNumber(), result.getProcessInstanceId());
                        metricsHelper.sendCustomEvent(salesOrder, SALES_ORDER_CONSUMED);
                        if (salesOrderSplit.isSplitted()) {
                            metricsHelper.sendCustomEvent(salesOrder, SPLIT_ORDER_GENERATED);
                        }
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
