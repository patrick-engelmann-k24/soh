package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Events;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;

@Component
public class CheckOrderCancelPossibleDelegate implements JavaDelegate {

    @Autowired
    private CamundaHelper camundaHelper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private SalesOrderRowService orderRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        String shipmentMethod = (String) delegateExecution.getVariable(SHIPMENT_METHOD.getName());
        boolean orderCancelPossible = true;
        if (camundaHelper.hasPassed(
                delegateExecution.getProcessInstanceId(),
                Events.MSG_ORDER_PAYMENT_SECURED.getName())) {
            // load all subprocess instances
            List<ProcessInstance> orderRowProcesses = runtimeService.createProcessInstanceQuery()
                    .superProcessInstanceId(delegateExecution.getProcessInstanceId())
                    .list();

            // check if one this is not able to cancel the whole order can not cancelled
            for (ProcessInstance orderRowProcess : orderRowProcesses) {
                if (!orderRowService.checkOrderRowCancellationPossible(orderRowProcess.getProcessInstanceId(), shipmentMethod)) {
                    orderCancelPossible = false;
                }
            }
        }

        delegateExecution.setVariable(Variables.ORDER_CANCEL_POSSIBLE.getName(), orderCancelPossible);
    }
}
