package de.kfzteile24.salesOrderHub.services.processmigration.handler.returnorderprocess.v14;

import de.kfzteile24.salesOrderHub.services.processmigration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.RETURN_ORDER_PROCESS;

@Component
@Migration(
        processDefinition = RETURN_ORDER_PROCESS,
        version = 14
)
@RequiredArgsConstructor
public class ReturnOrderMigrationV14 extends AbstractMigrationHandler {

    @Override
    public MigrationPlan createModelMigrationPlan(String sourceProcessDefinitionId, String targetProcessDefinitionId) {
        return processEngine.getRuntimeService()
                .createMigrationPlan(sourceProcessDefinitionId, targetProcessDefinitionId)
                .mapEqualActivities()
                .updateEventTriggers()
                .build();
    }
}
