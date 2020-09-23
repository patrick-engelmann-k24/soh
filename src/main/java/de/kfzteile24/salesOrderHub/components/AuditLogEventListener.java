package de.kfzteile24.salesOrderHub.components;

import de.kfzteile24.salesOrderHub.domain.AuditableEntity;
import de.kfzteile24.salesOrderHub.domain.types.AuditLogAction;
import de.kfzteile24.salesOrderHub.services.AuditLogService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.persistence.*;
import javax.validation.constraints.NotNull;

@RequiredArgsConstructor
@AllArgsConstructor
public class AuditLogEventListener {

    @NotNull
    AuditLogService auditLogService;

    @PostPersist
    public void prePersist(Object entity) { // Persistence logic
        this.storeEvent(entity, AuditLogAction.CREATE);
    }

    @PostUpdate
    public void preUpdate(Object entity) { //Updation logic
        this.storeEvent(entity, AuditLogAction.UPDATE);
    }

    @PostRemove
    public void preRemove(Object entity) { //Removal logic
        this.storeEvent(entity, AuditLogAction.DELETE);
    }

    private void storeEvent(Object entity, AuditLogAction logAction) {
        auditLogService.createLog((AuditableEntity) entity, logAction);
    }

}
