package de.kfzteile24.salesOrderHub.services.processmigration.handler.returnorderprocess.v14.step;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.SalesOrderReturn;
import de.kfzteile24.salesOrderHub.services.SalesOrderReturnService;
import de.kfzteile24.salesOrderHub.services.processmigration.ProcessQueryService;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.camunda.bpm.engine.runtime.VariableInstance;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.camunda.bpm.extension.migration.plan.step.StepExecutionContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ModificationStepV14 implements Step {

    private final ProcessQueryService processQueryService;
    private final SalesOrderReturnService salesOrderReturnService;

    @Override
    public void perform(StepExecutionContext stepExecutionContext) {
        val sourceProcessInstanceId = stepExecutionContext.getProcessInstanceId();
        List<VariableInstance> sourceProcessVariableInstances = processQueryService.getVariableInstances(sourceProcessInstanceId);
        String orderNumber = processQueryService.getVariableValue(sourceProcessVariableInstances, Variables.ORDER_NUMBER.getName());

        val salesOrderReturn = createExample(orderNumber);
        if (salesOrderReturnService.getCountByExample(salesOrderReturn) > 1) {
            salesOrderReturnService.getFirstByOrderNumber(orderNumber)
                    .ifPresent(salesOrderReturnService::delete);
        }
    }

    private static SalesOrderReturn createExample(String orderNumber) {
        return SalesOrderReturn.builder()
                .orderNumber(orderNumber)
                .build();
    }
}
