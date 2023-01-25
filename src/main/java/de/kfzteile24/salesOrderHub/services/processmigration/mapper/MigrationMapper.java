package de.kfzteile24.salesOrderHub.services.processmigration.mapper;

import de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition;
import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import org.camunda.bpm.extension.migration.plan.ProcessDefinitionSpec;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface MigrationMapper {

    default ProcessMigration map(Migration migration) {
        return ProcessMigration.builder()
                .processDefinition(migration.processDefinition())
                .version(migration.version())
                .build();
    }

    ProcessMigration map(ProcessDefinition processDefinition, int version);

    default ProcessMigration map(org.camunda.bpm.engine.repository.ProcessDefinition processDefinition) {
        return map(ProcessDefinition.fromName(processDefinition.getKey()), processDefinition.getVersion());
    }

    ProcessDefinitionSpec map(String processDefinitionKey, int processDefinitionVersion);
}
