package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v15.step;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.camunda.bpm.extension.migration.plan.step.StepExecutionContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModificationStepV15 implements Step {

    @Override
    public void perform(StepExecutionContext stepExecutionContext) {
        // TBD
    }
}
