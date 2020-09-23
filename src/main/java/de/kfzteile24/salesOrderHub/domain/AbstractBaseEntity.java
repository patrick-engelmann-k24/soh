package de.kfzteile24.salesOrderHub.domain;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.domain.Persistable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public abstract class AbstractBaseEntity implements Persistable<UUID> {

    @Id
//    @Column(name = "id", unique = true, nullable = false, columnDefinition = "uuid", updatable = false)
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    @GeneratedValue
//    @GeneratedValue(generator = "system-uuid")
//    @GenericGenerator(name = "system-uuid", strategy = "uuid")
//    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    private UUID id;

    public UUID getId() {
        return this.id;
    }

    @Override
    public boolean isNew() {
        return this.id == null;
    }
}
