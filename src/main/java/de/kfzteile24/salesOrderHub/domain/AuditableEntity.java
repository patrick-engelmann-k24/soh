package de.kfzteile24.salesOrderHub.domain;

import java.util.UUID;

public interface AuditableEntity {
    /**
     * returns the name of the entity
     *
     * @return
     */
    String getEntity();

    UUID getId();
}
