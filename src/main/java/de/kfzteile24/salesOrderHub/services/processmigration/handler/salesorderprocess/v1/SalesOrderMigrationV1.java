package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v1;

import de.kfzteile24.salesOrderHub.services.processmigration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v1.step.ModificationStepV1;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Component
@Migration(
        processDefinition = SALES_ORDER_PROCESS,
        version = 1
)
@RequiredArgsConstructor
public class SalesOrderMigrationV1 extends AbstractMigrationHandler {

    private final ModificationStepV1 modificationStep;

    @Override
    public Collection<Step> getMigrationSteps() {
        return List.of(modificationStep);
    }

    @Override
    public MigrationPlan createModelMigrationPlan(String sourceProcessDefinitionId, String targetProcessDefinitionId) {
        return processEngine.getRuntimeService()
                .createMigrationPlan(sourceProcessDefinitionId, targetProcessDefinitionId)
                .mapEqualActivities()
                .build();
    }
}
