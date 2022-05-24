package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder.listener;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.DropshipmentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CheckIsDropshipmentOrderListener implements ExecutionListener {

    private final DropshipmentOrderService dropshipmentOrderService;

    @Override
    public void notify(DelegateExecution delegateExecution) throws Exception {
        var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        delegateExecution.setVariable(Variables.IS_DROPSHIPMENT_ORDER.getName(),
                dropshipmentOrderService.isDropShipmentOrder(orderNumber));
    }
}
