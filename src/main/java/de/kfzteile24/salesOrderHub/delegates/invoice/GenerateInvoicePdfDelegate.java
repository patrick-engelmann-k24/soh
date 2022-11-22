package de.kfzteile24.salesOrderHub.delegates.invoice;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundCustomException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenerateInvoicePdfDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundCustomException("Could not find order with orderNumber: " + orderNumber
                        + " for generating invoice pdf"));
        snsPublishService.publishInvoicePdfGenerationTriggeredEvent(salesOrder.getLatestJson());
        log.info("{} delegate invoked", GenerateInvoicePdfDelegate.class.getSimpleName());
    }

}
