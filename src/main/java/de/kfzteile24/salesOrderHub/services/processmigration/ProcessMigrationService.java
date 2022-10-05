package de.kfzteile24.salesOrderHub.services.processmigration;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import de.kfzteile24.salesOrderHub.services.processmigration.exception.NoMigrationHandlerFound;
import de.kfzteile24.salesOrderHub.services.processmigration.mapper.MigrationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessMigrationService {

    private final Collection<MigrationHandler> migrationHandlers;
    private final MigrationMapper migrationMapper;
    private final ProcessQueryService processQueryService;

    @Async("asyncExecutor")
    public void executeMigration(ProcessMigration processMigration, boolean migrateParent) {
        var processMigrations = new ArrayList<ProcessMigration>();
        processMigrations.add(processMigration);

        if (migrateParent) {
            createParentMigrationPlanIfAny(processMigration)
                    .ifPresent(processMigrations::add);
        }

        migrate(processMigrations);
    }

    private Optional<ProcessMigration> createParentMigrationPlanIfAny(ProcessMigration subProcessMigration) {
        var subProcessProcessDefinitionId =
                processQueryService.getProcessDefinitionId(subProcessMigration.getProcessDefinition().getName(),
                        subProcessMigration.getVersion());
        var subProcessProcessInstance =
                processQueryService.getAnyExecutionByProcessDefinitionId(subProcessProcessDefinitionId);
        return Optional.ofNullable(subProcessProcessInstance)
                .filter(this::hasParentProcess)
                .map(ProcessInstance::getRootProcessInstanceId)
                .map(parentProcessInstanceId -> {
                        var parentProcessInstance =
                                processQueryService.getProcessInstance(parentProcessInstanceId);
                        var parentProcessDefinition =
                                processQueryService.getProcessDefinition(parentProcessInstance.getProcessDefinitionId());
                        return migrationMapper.map(parentProcessDefinition);
                    });
    }

    private boolean hasParentProcess(ProcessInstance processInstance) {
        return !StringUtils.equals(processInstance.getId(), processInstance.getRootProcessInstanceId());
    }

    private void migrate(Collection<ProcessMigration> processMigrations) {
        processMigrations.forEach(processMigration ->
            migrationHandlers.stream()
                    .filter(migrationHandler -> supportsMigration(processMigration, migrationHandler))
                    .findFirst()
                    .ifPresentOrElse(migrationHandler -> migrationHandler.migrate(processMigration), () -> {
                        throw new NoMigrationHandlerFound(processMigration);
                    })
        );
    }

    private boolean supportsMigration(ProcessMigration processMigration, MigrationHandler migrationHandler) {
        var migrationAnnotation = migrationHandler.getClass().getAnnotation(Migration.class);
        var existingProcessMigration = migrationMapper.map(migrationAnnotation);
        return Objects.equals(processMigration, existingProcessMigration);
    }
}
