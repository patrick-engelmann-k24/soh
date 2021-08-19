package de.kfzteile24.salesOrderHub.domain.audit;

import de.kfzteile24.salesOrderHub.domain.converter.AuditActionConverter;
import de.kfzteile24.salesOrderHub.domain.converter.OrderJsonConverter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for storing our audit events
 */
@Entity
@Table(name = "audit_log", schema = "public", catalog = "soh")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Data
@Builder
public class AuditLog implements Persistable<UUID> {

    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    @GeneratedValue
    private UUID id;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "action")
    @Convert(converter = AuditActionConverter.class)
    private Action action;

    @Column(name = "data", columnDefinition = "json")
    @Convert(converter = OrderJsonConverter.class)
    private Object data;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Override
    public boolean isNew() {
        return this.id == null;
    }
}
