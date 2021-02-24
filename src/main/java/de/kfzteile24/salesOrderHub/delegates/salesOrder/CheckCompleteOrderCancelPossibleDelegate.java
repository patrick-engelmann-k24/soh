package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CheckCompleteOrderCancelPossibleDelegate implements JavaDelegate {

    @Autowired
    CamundaHelper camundaHelper;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        boolean orderCancelPossible = true;
        if (camundaHelper.hasPassed(
                delegateExecution.getProcessInstanceId(),
                Events.MSG_ORDER_PAYMENT_SECURED.getName())) {
            orderCancelPossible = false;
        }

        delegateExecution.setVariable(Variables.ORDER_CANCEL_POSSIBLE.getName(), orderCancelPossible);
    }
}
