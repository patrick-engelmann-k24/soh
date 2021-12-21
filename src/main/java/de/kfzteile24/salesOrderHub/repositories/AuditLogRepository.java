package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * @author vinaya
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findBySalesOrderId(@Param("salesOrderId") UUID salesOrderId);
}
