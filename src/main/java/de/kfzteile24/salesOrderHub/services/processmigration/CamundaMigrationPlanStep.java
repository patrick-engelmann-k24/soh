package de.kfzteile24.salesOrderHub.services.processmigration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.camunda.bpm.extension.migration.plan.step.StepExecutionContext;
import org.camunda.bpm.extension.migration.plan.step.model.MigrationPlanFactory;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class CamundaMigrationPlanStep implements Step {

    private final MigrationPlanFactory migrationPlanFactory;

    @Override
    public void perform(StepExecutionContext stepExecutionContext) {
        try {
            MigrationPlan migrationPlan = migrationPlanFactory.apply(stepExecutionContext.getSourceProcessDefinitionId(),
                    stepExecutionContext.getTargetProcessDefinitionId());
            Optional.ofNullable(migrationPlan)
                    .ifPresent(plan -> stepExecutionContext.getProcessEngine().getRuntimeService()
                            .newMigration(plan)
                            .processInstanceIds(stepExecutionContext.getProcessInstanceId())
                            .execute());
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
