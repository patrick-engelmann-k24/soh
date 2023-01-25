package de.kfzteile24.salesOrderHub.services.processmigration;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.extension.migration.plan.step.Step;

import java.util.List;

public interface MigrationHandler {

    MigrationPlan createModelMigrationPlan(String sourceProcessDefinitionId, String targetProcessDefinitionId);

    List<Step> getPostMigrationSteps();

    List<Step> getPreMigrationSteps();

    void migrate(ProcessMigration processMigration);
}

