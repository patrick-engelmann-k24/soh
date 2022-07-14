package de.kfzteile24.salesOrderHub.services.migration.handler.salesorderprocess.v16;

import de.kfzteile24.salesOrderHub.services.migration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.migration.Migration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Component
@Migration(
        processDefinition = SALES_ORDER_PROCESS,
        version = 16
)
@RequiredArgsConstructor
public class SalesOrderMigrationV16 extends AbstractMigrationHandler {
}
