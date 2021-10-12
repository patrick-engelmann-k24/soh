package de.kfzteile24.salesOrderHub.delegates.salesOrder;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.domain.audit.Action;
import de.kfzteile24.salesOrderHub.exception.SalesOrderNotFoundException;
import de.kfzteile24.salesOrderHub.services.SalesOrderService;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import static javax.transaction.Transactional.TxType.REQUIRES_NEW;

@Component
@RequiredArgsConstructor
public class OrderCancelledDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @NonNull
    private final SalesOrderService salesOrderService;

    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        handleOrderCancellation(orderNumber);

        final var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));
        final var latestJson = salesOrder.getLatestJson();

        snsPublishService.publishOrderRowsCancelled(latestJson, latestJson.getOrderRows(), true);
    }

    @Transactional(REQUIRES_NEW)
    protected void handleOrderCancellation(String orderNumber) {
        var salesOrder = salesOrderService.getOrderByOrderNumber(orderNumber)
                .orElseThrow(() -> new SalesOrderNotFoundException("Could not find order: " + orderNumber));

        final var latestJson = salesOrder.getLatestJson();
        latestJson.getOrderRows().forEach(orderRow -> {
            orderRow.setIsCancelled(true);
        });

        salesOrderService.save(salesOrder, Action.ORDER_CANCELLED);
    }

}
