package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v1.step;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.camunda.bpm.extension.migration.plan.step.StepExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModificationStepV1 implements Step {

    @Override
    public void perform(StepExecutionContext stepExecutionContext) {
        var processInstanceId = stepExecutionContext.getProcessInstanceId();

        stepExecutionContext.getProcessEngine().getRuntimeService()
                .createProcessInstanceModification(processInstanceId)
                .cancelAllForActivity("eventMsgOrderPaymentSecured")
                .startAfterActivity("eventThrowMsgOrderCreated")
                .execute();
    }
}
