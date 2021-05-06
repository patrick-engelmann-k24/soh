package de.kfzteile24.salesOrderHub.helper;

import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.domain.audit.AuditLog;
import de.kfzteile24.salesOrderHub.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Component
@RequiredArgsConstructor
public class AuditLogUtil {
    private final AuditLogRepository auditLogRepository;

    public void assertAuditLogExists(UUID id, Action action) {
        assertAuditLogExists(id, action, 1);
    }

    public void assertAuditLogExists(UUID id, Action action, int count) {
        final List<AuditLog> auditLogs = getAuditLogs(id, action);

        assertThat(auditLogs.size()).isEqualTo(count);
    }

    public void assertAuditLogDoesNotExist(UUID id, Action action) {
        final List<AuditLog> auditLogs = getAuditLogs(id, action);

        assertThat(auditLogs.isEmpty()).isTrue();
    }

    private List<AuditLog> getAuditLogs(UUID id, Action action) {
        return auditLogRepository.findBySalesOrderId(id).stream()
                .filter(log -> action.equals(log.getAction()))
                .collect(Collectors.toList());
    }

}
