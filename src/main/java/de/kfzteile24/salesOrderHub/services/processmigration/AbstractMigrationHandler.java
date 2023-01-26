package de.kfzteile24.salesOrderHub.services.processmigration;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import de.kfzteile24.salesOrderHub.services.processmigration.mapper.MigrationMapper;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.extension.migration.Migrator;
import org.camunda.bpm.extension.migration.plan.step.model.MigrationPlanFactory;
import org.camunda.bpm.extension.migration.plan.step.variable.strategy.ReadProcessVariable;
import org.camunda.bpm.extension.migration.plan.step.variable.strategy.WriteProcessVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public abstract class AbstractMigrationHandler implements MigrationHandler {

    protected ProcessEngine processEngine;
    protected ReadProcessVariable readProcessVariable;
    protected WriteProcessVariable writeProcessVariable;
    protected ProcessQueryService processQueryService;
    private Migrator migrator;
    private MigrationMapper migrationMapper;

    public void migrate(ProcessMigration processMigration) {
        var processDefinition = processMigration.getProcessDefinition().getName();
        var sourceVersion = processMigration.getVersion();
        var targetVersion = processQueryService.getProcessDefinitionLastVersion(processDefinition);
        var sourceProcessDefinitionId = processQueryService.getProcessDefinitionId(processDefinition, sourceVersion);
        var targetProcessDefinitionId = processQueryService.getProcessDefinitionId(processDefinition, targetVersion);
        MigrationPlanFactory camundaMigrationPlan =
                (source, target) -> createModelMigrationPlan(sourceProcessDefinitionId, targetProcessDefinitionId);
        var migrationPlan = org.camunda.bpm.extension.migration.plan.MigrationPlan.builder()
                .from(migrationMapper.map(processDefinition, sourceVersion))
                .to(migrationMapper.map(processDefinition, targetVersion))
                .steps(getMigrationSteps())
                .step(new CamundaMigrationPlanStep(camundaMigrationPlan))
                .build();
        applyMigration(migrationPlan);
    }

    public void applyMigration(org.camunda.bpm.extension.migration.plan.MigrationPlan migrationPlan) {
        migrator.migrate(migrationPlan);
    }

    @Autowired
    private void init(ProcessEngine processEngine, Migrator migrator, MigrationMapper migrationMapper,
                      ReadProcessVariable readProcessVariable, WriteProcessVariable writeProcessVariable,
                      ProcessQueryService processQueryService) {
        this.processEngine = processEngine;
        this.migrator = migrator;
        this.migrationMapper = migrationMapper;
        this.readProcessVariable = readProcessVariable;
        this.writeProcessVariable = writeProcessVariable;
        this.processQueryService = processQueryService;
    }
}

