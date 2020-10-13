package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Date;
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
@EqualsAndHashCode(callSuper = true)
@ToString()
public class AuditLog extends AbstractBaseEntity {
    @Basic
    @Column(name = "entity")
    private String entity;

    @Basic
    @Column(name = "action")
    private String action;

    @Basic
    @Column(name = "entity_id")
    private UUID entityId;

    @Basic
    @Column(name = "origin_data")
    private String originData;


    @Basic
    @Column(name = "created_by")
    private String createdBy;

    @Basic
    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date createdAt;

    @Basic
    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Basic
    @Column(name = "last_modified_date")
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date lastModifiedDate;

}
