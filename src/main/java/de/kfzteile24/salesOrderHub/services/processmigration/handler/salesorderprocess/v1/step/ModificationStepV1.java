package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v1.step;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.domain.SalesOrder;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.processmigration.ProcessQueryService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.camunda.bpm.extension.migration.plan.step.StepExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ModificationStepV1 implements Step {

    private final ProcessQueryService processQueryService;
    private final SalesOrderService salesOrderService;
    private final CamundaHelper camundaHelper;

    @Override
    public void perform(StepExecutionContext stepExecutionContext) {
        var processInstanceId = stepExecutionContext.getProcessInstanceId();
        List<VariableInstance> processVariableInstances = processQueryService.getVariableInstances(processInstanceId);
        String orderNumber = processQueryService.getVariableValue(processVariableInstances, Variables.ORDER_NUMBER.getName());
        SalesOrder salesOrder = salesOrderService.updateProcessInstanceId(orderNumber, processInstanceId);

        stepExecutionContext.getProcessEngine().getRuntimeService()
                .createProcessInstanceModification(processInstanceId)
                .cancelAllForActivity("eventMsgOrderPaymentSecured")
                .startAfterActivity("eventThrowMsgOrderCreated")
                .setVariables(camundaHelper.createProcessVariables(salesOrder))
                .execute();
    }
}
