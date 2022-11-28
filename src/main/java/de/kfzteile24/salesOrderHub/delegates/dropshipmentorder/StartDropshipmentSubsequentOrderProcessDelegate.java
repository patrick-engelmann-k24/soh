package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartDropshipmentSubsequentOrderProcessDelegate implements JavaDelegate {
    private final SalesOrderService salesOrderService;
    private final DropshipmentOrderService dropshipmentOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        log.info("{} delegate invoked", StartDropshipmentSubsequentOrderProcessDelegate.class.getSimpleName());

        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        dropshipmentOrderService.startDropshipmentSubsequentOrderProcess(salesOrder);
    }
}
