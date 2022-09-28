package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v17;

import de.kfzteile24.salesOrderHub.services.processmigration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Component
@Migration(
        processDefinition = SALES_ORDER_PROCESS,
        version = 17
)
@RequiredArgsConstructor
public class SalesOrderMigrationV17 extends AbstractMigrationHandler {
}
