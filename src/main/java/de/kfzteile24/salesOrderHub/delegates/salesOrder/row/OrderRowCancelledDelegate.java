package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.row.RowVariables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderRowService;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRowCancelledDelegate implements JavaDelegate {

    @NonNull
    private final SalesOrderService salesOrderService;

    @NonNull
    private final SalesOrderRowService salesOrderRowService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var orderRowId = (String) delegateExecution.getVariable(RowVariables.ORDER_ROW_ID.getName());

        handleOrderRowCancellationInDedicatedTransaction(orderNumber, orderRowId);

        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        salesOrderRowService.publishOrderRowsCancelled(orderRowId, salesOrder);
    }

    @Transactional(REQUIRES_NEW)
    protected void handleOrderRowCancellationInDedicatedTransaction(String orderNumber, String orderRowId) {
        salesOrderRowService.markOrderRowsAsCancelled(orderNumber, orderRowId);
    }

}
