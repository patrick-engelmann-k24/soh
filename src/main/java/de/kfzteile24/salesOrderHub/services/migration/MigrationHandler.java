package de.kfzteile24.salesOrderHub.services.migration;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.extension.migration.plan.step.Step;

import java.util.List;

public interface MigrationHandler {

    MigrationPlan createModelMigrationPlan(String sourceProcessDefinitionId, String targetProcessDefinitionId);

    List<Step> getMigrationSteps();

    void migrate(ProcessMigration processMigration);
}

