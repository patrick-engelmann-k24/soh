package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.AuditLog;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface AuditLogRepository extends CrudRepository<AuditLog, UUID> {
}
