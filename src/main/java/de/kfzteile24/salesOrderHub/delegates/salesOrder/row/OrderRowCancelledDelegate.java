package de.kfzteile24.salesOrderHub.delegates.salesOrder.row;

import de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables;
import de.kfzteile24.salesOrderHub.services.SnsPublishService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_ROW_ID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderRowCancelledDelegate implements JavaDelegate {

    @NonNull
    private final SnsPublishService snsPublishService;

    @Override
    @Transactional
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(Variables.ORDER_NUMBER.getName());
        final var orderRowId = (String) delegateExecution.getVariable(ORDER_ROW_ID.getName());
        log.info("Order row process with order number {} and order row: {} is cancelled", orderNumber, orderRowId);
        snsPublishService.publishOrderRowCancelled(orderNumber, orderRowId);
    }

}
