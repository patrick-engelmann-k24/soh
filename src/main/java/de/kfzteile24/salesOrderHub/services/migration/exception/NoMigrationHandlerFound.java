package de.kfzteile24.salesOrderHub.services.migration.exception;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;

public class NoMigrationHandlerFound extends RuntimeException {

    public NoMigrationHandlerFound(ProcessMigration processMigration) {
        super(processMigration.toString());
    }
}
