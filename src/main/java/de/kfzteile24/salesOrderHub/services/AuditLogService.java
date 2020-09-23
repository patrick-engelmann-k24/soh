package de.kfzteile24.salesOrderHub.services;

import de.kfzteile24.salesOrderHub.domain.AuditLog;
import de.kfzteile24.salesOrderHub.domain.AuditableEntity;
import de.kfzteile24.salesOrderHub.domain.types.AuditLogAction;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Class that can create a log entry
 *
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {
    @NotNull
    private final AuditLogRepository auditLogRepository;

    @PostPersist
    public void prePersist(Object entity) { // Persistence logic
        this.createLog((AuditableEntity)entity, AuditLogAction.CREATE);
    }

    @PostUpdate
    public void preUpdate(Object entity) { //Updation logic
        this.createLog((AuditableEntity)entity, AuditLogAction.UPDATE);
    }

    @PostRemove
    public void preRemove(Object entity) { //Removal logic
        this.createLog((AuditableEntity) entity, AuditLogAction.DELETE);
    }

    public void createLog(AuditableEntity e, AuditLogAction action) {
        final AuditLog auditLog = AuditLog.builder()
                .entity(e.getEntity())
                .entityId(e.getId())
                .action(action.name())
                // todo replace after GSON setup with the json string.
                .originData(e.toString())
                .lastModifiedDate(new Date())
                .build();
        auditLogRepository.save(auditLog);
    }
}
