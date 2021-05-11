package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;


import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UpdateOrderEntityListener implements ExecutionListener {

    @NonNull
    private final SalesOrderService salesOrderService;

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        String orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        Optional<SalesOrder> salesOrderOptional = salesOrderService.getOrderByOrderNumber(orderNumber);
        if (salesOrderOptional.isPresent()) {
            SalesOrder salesOrder = salesOrderOptional.get();

            salesOrder.setProcessId(delegateExecution.getProcessInstanceId());
            salesOrderService.updateOrder(salesOrder);
        }
    }
}
