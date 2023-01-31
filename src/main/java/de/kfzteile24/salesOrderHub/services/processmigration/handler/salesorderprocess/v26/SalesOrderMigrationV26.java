package de.kfzteile24.salesOrderHub.services.processmigration.handler.salesorderprocess.v26;

import de.kfzteile24.salesOrderHub.dto.migration.ProcessMigration;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.processmigration.AbstractMigrationHandler;
import de.kfzteile24.salesOrderHub.services.processmigration.Migration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.kfzteile24.salesOrderHub.constants.bpmn.ProcessDefinition.SALES_ORDER_PROCESS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Activities.PERSIST_DROPSHIPMENT_ORDER_ITEMS;
import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Component
@Migration(
        processDefinition = SALES_ORDER_PROCESS,
        version = 26
)
@RequiredArgsConstructor
@Slf4j
public class SalesOrderMigrationV26 extends AbstractMigrationHandler {

    private static final List<String> ORDER_NUMBERS_TO_MIGRATE = List.of(
            "538055746-1",
            "528207949-1",
            "538056943",
            "538052479-1",
            "538051860",
            "538055363",
            "538056762");

    private final SalesOrderService salesOrderService;

    @Override
    public void migrate(ProcessMigration processMigration) {
        ORDER_NUMBERS_TO_MIGRATE.forEach(this::createSalesOrderProcess);
    }

    private void createSalesOrderProcess(String orderNumber) {
        try {
            ProcessInstance targetProcessInstance = processEngine.getRuntimeService()
                    .createProcessInstanceByKey(SALES_ORDER_PROCESS.getName())
                    .startBeforeActivity(PERSIST_DROPSHIPMENT_ORDER_ITEMS.getName())
                    .setVariable(ORDER_NUMBER.getName(), orderNumber)
                    .businessKey(orderNumber)
                    .execute();

            salesOrderService.updateProcessInstanceId(orderNumber, targetProcessInstance.getProcessInstanceId());
            log.info("SalesOrder process migration done for order number '{}'", orderNumber);
        } catch (Exception e) {
            log.error("SalesOrder process migration failed for order number '{}'. Error: {}", orderNumber, e.getLocalizedMessage());
        }
    }
}
