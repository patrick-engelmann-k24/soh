package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.PaymentType;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.ShipmentMethod;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.dto.split.SalesOrderSplit;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.helper.OrderUtil;
import de.kfzteile24.salesOrderHub.helper.SleuthHelper;
import de.kfzteile24.salesOrderHub.services.sqs.EnrichMessageForDlq;
import de.kfzteile24.salesOrderHub.services.sqs.MessageWrapper;
import de.kfzteile24.soh.order.dto.Order;
import de.kfzteile24.soh.order.dto.OrderRows;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SALES_ORDER_CONSUMED;
import static de.kfzteile24.salesOrderHub.constants.CustomEventName.SPLIT_ORDER_GENERATED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.NEW;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.CustomerType.RECURRING;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages.ORDER_RECEIVED_ECP;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.CUSTOMER_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.IS_ORDER_CANCELLED;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROWS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PAYMENT_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.PLATFORM_TYPE;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SALES_CHANNEL;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.VIRTUAL_ORDER_ROWS;

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
    private final SleuthHelper sleuthHelper;

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
                ProcessInstance result = createOrderProcess(createdSalesOrder, ORDER_RECEIVED_ECP);

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

    public ProcessInstance createOrderProcess(SalesOrder salesOrder, Messages message) {
        sleuthHelper.updateTraceId(salesOrder.getOrderNumber());

        return camundaHelper.correlateMessage(message,
                salesOrder.getOrderNumber(),
                createProcessVariables(salesOrder))
                .getProcessInstance();
    }

    public static Map<String, Object> createProcessVariables(SalesOrder salesOrder) {
        final String orderNumber = salesOrder.getOrderNumber();

        List<String> orderRowSkus;
        List<String> virtualOrderRowSkus = new ArrayList<>();
        String shippingType;
        String paymentType;
        String platformType;
        orderRowSkus = new ArrayList<>();
        final var order = salesOrder.getLatestJson();
        platformType = order.getOrderHeader().getPlatform().name();
        paymentType = PaymentType.getPaymentType(order.getOrderHeader().getPayments());
        shippingType = order.getOrderRows().get(0).getShippingType();
        for (OrderRows orderRow : order.getOrderRows()) {
            if (ShipmentMethod.isShipped(orderRow.getShippingType())) {
                shippingType = orderRow.getShippingType();
                orderRowSkus.add(orderRow.getSku());
            } else {
                virtualOrderRowSkus.add(orderRow.getSku());
            }
        }


        final Map<String, Object> processVariables = new HashMap<>();
        processVariables.put(SHIPMENT_METHOD.getName(), shippingType);
        processVariables.put(PLATFORM_TYPE.getName(), platformType);
        processVariables.put(ORDER_NUMBER.getName(), orderNumber);
        processVariables.put(PAYMENT_TYPE.getName(), paymentType);
        processVariables.put(ORDER_ROWS.getName(), orderRowSkus);
        processVariables.put(IS_ORDER_CANCELLED.getName(), false);
        processVariables.put(CUSTOMER_TYPE.getName(),
                salesOrder.isRecurringOrder() ? RECURRING.getType() : NEW.getType());
        processVariables.put(SALES_CHANNEL.getName(), salesOrder.getSalesChannel());

        if (!virtualOrderRowSkus.isEmpty()) {
            processVariables.put(VIRTUAL_ORDER_ROWS.getName(), virtualOrderRowSkus);
        }

        return processVariables;
    }
}
