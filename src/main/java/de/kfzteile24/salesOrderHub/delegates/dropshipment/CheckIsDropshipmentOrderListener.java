package de.kfzteile24.salesOrderHub.delegates.dropshipment;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.soh.order.dto.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.FulfillmentType.DELTICOM;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckIsDropshipmentOrderListener implements ExecutionListener {

    private final SalesOrderService salesOrderService;

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        var originalOrder = (Order) salesOrder.getOriginalOrder();
        var orderFulfillment = originalOrder.getOrderHeader().getOrderFulfillment();

        delegateExecution.setVariable(Variables.IS_DROPSHIPMENT_ORDER.getName(), isDropShipmentOrder(orderFulfillment));
    }

    private boolean isDropShipmentOrder(String orderFulfillment) {
        return StringUtils.equalsIgnoreCase(orderFulfillment, DELTICOM.getName());
    }
}
