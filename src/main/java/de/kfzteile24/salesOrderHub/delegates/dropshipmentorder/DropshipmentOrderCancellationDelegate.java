package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DropshipmentOrderCancellationDelegate implements JavaDelegate {

    private final DropshipmentOrderService dropshipmentOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        var processInstanceId = delegateExecution.getProcessInstanceId();
        dropshipmentOrderService.handleDropShipmentOrderCancellation(orderNumber, processInstanceId);
    }
}
