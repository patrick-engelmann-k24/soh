package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import lombok.extern.java.Log;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.SHIPMENT_METHOD;


@Component
@Log
public class CheckRowCancellationPossible implements JavaDelegate {

    @Autowired
    private SalesOrderRowService rowService;

    /**
     * Check if process (item) cancellation is possible
     *
     * @param delegateExecution ProcessInstance object
     */
    @Override
    public void execute(DelegateExecution delegateExecution) {
        final String shipmentMethod = (String) delegateExecution.getVariable(SHIPMENT_METHOD.getName());
        Boolean checkResult = rowService.checkItemCancellationPossible(delegateExecution.getProcessInstanceId(), shipmentMethod);

        delegateExecution.setVariable(RowVariables.ROW_CANCELLATION_POSSIBLE.getName(), checkResult);
    }
}
