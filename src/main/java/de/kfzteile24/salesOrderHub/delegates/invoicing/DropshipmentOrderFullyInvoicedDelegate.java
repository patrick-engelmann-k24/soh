package de.kfzteile24.salesOrderHub.delegates.invoicing;

import de.kfzteile24.salesOrderHub.constants.CustomEventName;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Messages;
import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.delegates.helper.CamundaHelper;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.helper.MetricsHelper;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DropshipmentOrderFullyInvoicedDelegate implements JavaDelegate {

    @NonNull
    private final CamundaHelper camundaHelper;
    @NonNull
    private final MetricsHelper metricsHelper;
    @NonNull
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        log.info("Dropshipment Order Fully Invoiced Delegate for order number {}", orderNumber);
        camundaHelper.correlateMessage(Messages.DROPSHIPMENT_ORDER_FULLY_INVOICED, orderNumber)
                .getProcessInstance();
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException(orderNumber));
        metricsHelper.sendCustomEventForDropshipmentOrder(salesOrder, CustomEventName.DROPSHIPMENT_ORDER_FULLY_INVOICED);
    }
}
