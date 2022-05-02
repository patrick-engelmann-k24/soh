package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CheckOrderCancelledDelegate implements ExecutionListener {

    @Autowired
    private SalesOrderRowService salesOrderRowService;

    @Autowired
    private SalesOrderService salesOrderService;

    @Override
    public void notify(DelegateExecution delegateExecution) {

        final String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        boolean isOrderCancelled = salesOrderRowService.cancelOrderProcessIfFullyCancelled(salesOrder);
        log.info("The order cancellation check has the following result. " +
                        "Order Number: {}, Is Order Cancelled?: {}",
                orderNumber, isOrderCancelled);
        delegateExecution.setVariable(Variables.IS_ORDER_CANCELLED.getName(), isOrderCancelled);
    }
}
