package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v6;

import de.kfzteile24.salesOrderHub.services.processmigration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Component
@Migration(
        processDefinition = SALES_ORDER_PROCESS,
        version = 6
)
@RequiredArgsConstructor
public class SalesOrderMigrationV6 extends AbstractMigrationHandler {

    @Override
    public MigrationPlan createModelMigrationPlan(String sourceProcessDefinitionId, String targetProcessDefinitionId) {
        return processEngine.getRuntimeService()
                .createMigrationPlan(sourceProcessDefinitionId, targetProcessDefinitionId)
                .mapEqualActivities()
                .build();
    }
}
