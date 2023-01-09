package de.kfzteile24.salesOrderHub.delegates.dropshipmentorder;

import de.kfzteile24.salesOrderHub.services.dropshipment.DropshipmentOrderRowService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import static de.kfzteile24.salesOrderHub.constants.bpmn.orderProcess.Variables.ORDER_NUMBER;

@Component
@Slf4j
@RequiredArgsConstructor
public class SaveDropshipmentOrderItemsDelegate implements JavaDelegate {

    @NonNull
    private final DropshipmentOrderRowService dropshipmentOrderRowService;


    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        final var orderNumber = (String) delegateExecution.getVariable(ORDER_NUMBER.getName());
        log.info("Save Dropshipment Order Items with quantities for orderNumber {}", orderNumber);
        dropshipmentOrderRowService.saveDropshipmentOrderItems(orderNumber);
    }
}
