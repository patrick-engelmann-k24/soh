package de.kfzteile24.salesOrderHub.services.processmigration;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import org.camunda.bpm.engine.migration.MigrationPlan;
import org.camunda.bpm.extension.migration.plan.step.Step;

import java.util.Collection;
import java.util.Collections;

public interface MigrationHandler {

    default MigrationPlan createModelMigrationPlan(String sourceProcessDefinitionId, String targetProcessDefinitionId) {
        return null;
    }

    default Collection<Step> getMigrationSteps() {
        return Collections.emptyList();
    }

    void migrate(ProcessMigration processMigration);
}

