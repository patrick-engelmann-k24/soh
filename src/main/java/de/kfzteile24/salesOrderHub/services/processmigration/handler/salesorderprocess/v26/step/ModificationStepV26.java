package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v26.step;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.processmigration.ProcessQueryService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.camunda.bpm.extension.migration.plan.step.StepExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.PERSIST_DROPSHIPMENT_ORDER_ITEMS;

@Component
@RequiredArgsConstructor
public class ModificationStepV26 implements Step {

    private final ProcessQueryService processQueryService;
    private final SalesOrderService salesOrderService;

    @Override
    public void perform(StepExecutionContext stepExecutionContext) {
        var sourceProcessInstanceId = stepExecutionContext.getProcessInstanceId();
        List<VariableInstance> sourceProcessVariableInstances = processQueryService.getVariableInstances(sourceProcessInstanceId);
        String orderNumber = processQueryService.getVariableValue(sourceProcessVariableInstances, Variables.ORDER_NUMBER.getName());

       stepExecutionContext.getProcessEngine().getRuntimeService()
                .deleteProcessInstance(sourceProcessInstanceId, "v26-instance-migration");

        ProcessInstance targetProcessInstance = stepExecutionContext.getProcessEngine().getRuntimeService()
                .createProcessInstanceByKey(SALES_ORDER_PROCESS.getName())
                .startBeforeActivity(PERSIST_DROPSHIPMENT_ORDER_ITEMS.getName())
                .setVariables(processQueryService.createVariablesFromSource(sourceProcessVariableInstances))
                .businessKey(orderNumber)
                .execute();

        salesOrderService.updateProcessInstanceId(orderNumber, targetProcessInstance.getProcessInstanceId());
    }
}
