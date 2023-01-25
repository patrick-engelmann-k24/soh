package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v26;

import de.kfzteile24.salesOrderHub.services.processmigration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v26.step.ModificationStepV26;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.extension.migration.plan.step.Step;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;

@Component
@Migration(
        processDefinition = SALES_ORDER_PROCESS,
        version = 26
)
@RequiredArgsConstructor
public class SalesOrderMigrationV26 extends AbstractMigrationHandler {

    private final ModificationStepV26 modificationStep;

    @Override
    public List<Step> getPreMigrationSteps() {
        return List.of(modificationStep);
    }
}
