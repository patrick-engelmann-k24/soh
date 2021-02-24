package de.kfzteile24.salesOrderHub.delegates.salesOrder.listener;


import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UpdateOrderEntityListener implements ExecutionListener {

    @Autowired
    private CamundaHelper helper;

    @Autowired
    private SalesOrderService salesOrderService;

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
